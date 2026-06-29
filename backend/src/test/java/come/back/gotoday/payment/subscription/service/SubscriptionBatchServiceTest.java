package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.external.toss.dto.TossAutomatedPaymentResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.payment.billing.entity.BillingInfo;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.plan.entity.Plan;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import come.back.gotoday.payment.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionBatchService 단위 테스트")
class SubscriptionBatchServiceTest {

    @InjectMocks
    private SubscriptionBatchService subscriptionBatchService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    private final Long SUBSCRIPTION_ID = 1L;
    private final LocalDate TODAY = LocalDate.of(2026, 6, 29);
    private final int GRACE_DAYS = 7;
    private final List<SubscriptionStatus> TARGET_STATUSES = List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED_PAYMENT_PENDING);

    @Nested
    @DisplayName("lockAndPreparePayment 메서드는")
    class Describe_lockAndPreparePayment {

        @Test
        @DisplayName("존재하지 않는 구독 ID로 조회 시 SUBSCRIPTION_NOT_FOUND 예외를 던진다.")
        void it_throws_exception_when_subscription_not_found() {
            // given
            when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionBatchService.lockAndPreparePayment(SUBSCRIPTION_ID, TODAY, TARGET_STATUSES, GRACE_DAYS))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        @Test
        @DisplayName("유예 기간이 만료된 구독인 경우, 구독을 취소 처리하고 실패 이력을 남긴 뒤 만료 파라미터를 반환한다.")
        void it_cancels_subscription_and_returns_expired_true_when_grace_period_expired() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            when(mockSubscription.getStatus()).thenReturn(SubscriptionStatus.EXPIRED_PAYMENT_PENDING);
            when(mockSubscription.isGracePeriodExpired(TODAY, GRACE_DAYS)).thenReturn(true);
            when(mockSubscription.getAmount()).thenReturn(9900L);

            when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // when
            SubscriptionBatchService.BatchPaymentParameters params =
                    subscriptionBatchService.lockAndPreparePayment(SUBSCRIPTION_ID, TODAY, TARGET_STATUSES, GRACE_DAYS);

            // then
            verify(mockSubscription, times(1)).cancel();
            verify(paymentHistoryRepository, times(1)).save(argThat(history ->
                    history.getOrderId().equals("ORD-FINAL-FAIL-" + SUBSCRIPTION_ID) &&
                            history.getFailureReason().contains("유예 기간 초과")
            ));
            assertThat(params.isExpired()).isTrue();
        }

        @Test
        @DisplayName("대상 상태(Target Status)가 아니거나 아직 결제일이 도래하지 않았다면 null을 반환하여 배치를 차단한다.")
        void it_returns_null_when_double_check_fails() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            when(mockSubscription.getStatus()).thenReturn(SubscriptionStatus.PAUSED); // 대상 상태가 아님 (ACTIVE, EXPIRED_PAYMENT_PENDING 외)

            when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // when
            SubscriptionBatchService.BatchPaymentParameters params =
                    subscriptionBatchService.lockAndPreparePayment(SUBSCRIPTION_ID, TODAY, TARGET_STATUSES, GRACE_DAYS);

            // then
            assertThat(params).isNull();
            verify(mockSubscription, never()).changeStatus();
        }

        @Test
        @DisplayName("모든 검증을 통과하면 상태를 PENDING으로 전환하고 강제 커밋한 뒤 정상 결제 파라미터 뭉치를 반환한다.")
        void it_updates_status_to_pending_and_returns_valid_parameters() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            BillingInfo mockBillingInfo = mock(BillingInfo.class);
            Plan mockPlan = mock(Plan.class);
            Member mockMember = mock(Member.class);

            // 스터빙 조립
            when(mockSubscription.getStatus()).thenReturn(SubscriptionStatus.ACTIVE);
            when(mockSubscription.getNextBillingDate()).thenReturn(TODAY);
            when(mockSubscription.getAmount()).thenReturn(9900L);
            when(mockSubscription.getBillingInfo()).thenReturn(mockBillingInfo);
            when(mockSubscription.getPlan()).thenReturn(mockPlan);

            when(mockBillingInfo.getCustomerKey()).thenReturn("cust-123");
            when(mockBillingInfo.getBillingKey()).thenReturn("bill-abc");
            when(mockBillingInfo.getMember()).thenReturn(mockMember);

            when(mockPlan.getDisplayName()).thenReturn("프리미엄 요금제");
            when(mockMember.getEmail()).thenReturn("user@test.com");
            when(mockMember.getNickname()).thenReturn("정산왕");

            when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // when
            SubscriptionBatchService.BatchPaymentParameters params =
                    subscriptionBatchService.lockAndPreparePayment(SUBSCRIPTION_ID, TODAY, TARGET_STATUSES, GRACE_DAYS);

            // then
            verify(mockSubscription, times(1)).changeStatus(); // PENDING 전이 검증
            verify(subscriptionRepository, times(1)).saveAndFlush(mockSubscription); // 동시성 격리 영속화 검증

            assertThat(params.isExpired()).isFalse();
            assertThat(params.getCustomerKey()).isEqualTo("cust-123");
            assertThat(params.getPlainBillingKey()).isEqualTo("bill-abc");
            assertThat(params.getSnapshotAmount()).isEqualTo(9900L);
            assertThat(params.getPlanName()).isEqualTo("프리미엄 요금제");
            assertThat(params.getOriginalStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("handleBatchPaymentFailure 메서드는")
    class Describe_handleBatchPaymentFailure {

        @Test
        @DisplayName("실패 데이터를 받으면 유예 상태로 구독을 전이시키고, 실패 사유가 255자를 초과하면 잘라내어 이력을 남긴다.")
        void it_updates_subscription_to_fail_and_truncates_long_failure_reason() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // 255글자가 넘어가는 매우 긴 오류 메시지 준비
            String longFailureReason = "A".repeat(300);

            // when
            subscriptionBatchService.handleBatchPaymentFailure(
                    SUBSCRIPTION_ID, "ORD-ERR-1", 9900L, longFailureReason, TODAY, SubscriptionStatus.ACTIVE
            );

            // then
            verify(mockSubscription, times(1)).changeToPaymentBatchFail(TODAY);
            verify(paymentHistoryRepository, times(1)).save(argThat(history ->
                    history.getFailureReason().endsWith("...") &&
                            history.getFailureReason().length() == 255
            ));
        }
    }

    @Nested
    @DisplayName("completeScheduledPayment 메서드는")
    class Describe_completeScheduledPayment {

        @Test
        @DisplayName("조회에 성공하면 구독을 다시 활성화(차기 결제일 갱신)하고 성공 장부를 기록한다.")
        void it_activates_subscription_and_saves_success_history() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            TossAutomatedPaymentResponse mockTossResponse = mock(TossAutomatedPaymentResponse.class);

            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));
            when(mockTossResponse.paymentKey()).thenReturn("toss-payment-key");
            when(mockTossResponse.totalAmount()).thenReturn(9900L);
            when(mockTossResponse.getReceiptUrl()).thenReturn("https://receipt.toss.com/123");

            // when
            subscriptionBatchService.completeScheduledPayment(SUBSCRIPTION_ID, "ORD-SUCCESS-1", mockTossResponse);

            // then
            verify(mockSubscription, times(1)).activate(); // 차기 이월 완료 검증
            verify(paymentHistoryRepository, times(1)).save(argThat(history ->
                    history.getPaymentKey().equals("toss-payment-key") &&
                            history.getAmount() == 9900L &&
                            history.getReceiptUrl().equals("https://receipt.toss.com/123")
            ));
        }
    }

    @Nested
    @DisplayName("finalizeSubscription 메서드는")
    class Describe_finalizeSubscription {

        @Test
        @DisplayName("구독 상태가 CANCELED_RESERVED(해지 예약) 상태가 아니라면 해지 요청을 무시한다.")
        void it_skips_finalizing_when_status_is_not_canceled_reserved() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            when(mockSubscription.getStatus()).thenReturn(SubscriptionStatus.ACTIVE); // 예약 상태 아님
            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // when
            subscriptionBatchService.finalizeSubscription(SUBSCRIPTION_ID);

            // then
            verify(mockSubscription, never()).cancel();
        }

        @Test
        @DisplayName("구독 상태가 해지 예약 상태라면 완벽히 즉시 해지(CANCELED) 상태로 전환한다.")
        void it_cancels_subscription_safely_when_status_is_canceled_reserved() {
            // given
            Subscription mockSubscription = mock(Subscription.class);
            when(mockSubscription.getStatus()).thenReturn(SubscriptionStatus.CANCELED_RESERVED);
            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(mockSubscription));

            // when
            subscriptionBatchService.finalizeSubscription(SUBSCRIPTION_ID);

            // then
            verify(mockSubscription, times(1)).cancel();
        }
    }
}