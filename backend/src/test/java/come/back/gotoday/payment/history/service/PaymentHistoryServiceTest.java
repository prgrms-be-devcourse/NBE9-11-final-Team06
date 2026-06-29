package come.back.gotoday.payment.history.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.history.dto.PaymentHistoryResponse;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.subscription.entity.Subscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentHistoryService 단위 테스트")
class PaymentHistoryServiceTest {

    @InjectMocks
    private PaymentHistoryService paymentHistoryService;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    private final Long MEMBER_ID = 1L;
    private final Long PAYMENT_HISTORY_ID = 100L;
    private final String MOCK_PAYMENT_KEY = "toss_validated_payment_key_2026";

    @Nested
    @DisplayName("getPaymentHistories 메서드는")
    class Describe_getPaymentHistories {

        @Test
        @DisplayName("회원 ID에 해당하는 결제 내역이 존재하면 DTO 리스트로 변환하여 반환한다.")
        void it_returns_payment_history_responses() {
            // given
            PaymentHistory mockHistory = mock(PaymentHistory.class);
            // static 메서드 PaymentHistoryResponse.from(history) 내부에서 Getter를 호출할 것이므로 Stubbing 처리
            when(mockHistory.getId()).thenReturn(PAYMENT_HISTORY_ID);
            when(mockHistory.getStatus()).thenReturn(PaymentStatus.SUCCESS);
            when(mockHistory.getAmount()).thenReturn(9900L);

            when(paymentHistoryRepository.findAllByMemberId(MEMBER_ID))
                    .thenReturn(List.of(mockHistory));

            // when
            List<PaymentHistoryResponse> result = paymentHistoryService.getPaymentHistories(MEMBER_ID);

            // then
            assertThat(result).hasSize(1);
            verify(paymentHistoryRepository, times(1)).findAllByMemberId(MEMBER_ID);
        }

        @Test
        @DisplayName("결제 내역이 전혀 없다면 빈 리스트를 반환한다.")
        void it_returns_empty_list_when_no_histories() {
            // given
            when(paymentHistoryRepository.findAllByMemberId(MEMBER_ID))
                    .thenReturn(Collections.emptyList());

            // when
            List<PaymentHistoryResponse> result = paymentHistoryService.getPaymentHistories(MEMBER_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPaymentKeyValidated 메서드는")
    class Describe_getPaymentKeyValidated {

        @Nested
        @DisplayName("존재하는 SUCCESS 상태의 결제 이력 ID가 주어지면")
        class Context_with_existing_success_payment {

            @Test
            @DisplayName("검증을 통과하고 평문 토스 paymentKey를 반환한다.")
            void it_returns_payment_key() {
                // given
                PaymentHistory mockHistory = mock(PaymentHistory.class);
                when(mockHistory.getStatus()).thenReturn(PaymentStatus.SUCCESS);
                when(mockHistory.getPaymentKey()).thenReturn(MOCK_PAYMENT_KEY);

                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.of(mockHistory));

                // when
                String paymentKey = paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID);

                // then
                assertThat(paymentKey).isEqualTo(MOCK_PAYMENT_KEY);
            }
        }

        @Nested
        @DisplayName("요청된 결제 이력 정보가 내부에 존재하지 않는다면")
        class Context_with_non_existent_payment_history {

            @Test
            @DisplayName("PAYMENT_HISTORY_NOT_FOUND 에러 예외를 발생시킨다.")
            void it_throws_payment_history_not_found_exception() {
                // given
                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_HISTORY_NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("결제 이력은 존재하지만 상태가 SUCCESS가 아니라면 (FAILED 등)")
        class Context_with_not_success_payment_status {

            @Test
            @DisplayName("CANNOT_CANCEL_FAILED_PAYMENT 에러 예외를 발생시킨다.")
            void it_throws_cannot_cancel_failed_payment_exception() {
                // given
                PaymentHistory mockHistory = mock(PaymentHistory.class);
                // SUCCESS가 아닌 상태 세팅 (Branch 커버리지 확보)
                when(mockHistory.getStatus()).thenReturn(PaymentStatus.FAILED);

                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.of(mockHistory));

                // when & then
                assertThatThrownBy(() -> paymentHistoryService.getPaymentKeyValidated(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CANCEL_FAILED_PAYMENT);
            }
        }
    }

    @Nested
    @DisplayName("cancelPaymentAndSubscription 메서드는")
    class Describe_cancelPaymentAndSubscription {

        @Nested
        @DisplayName("올바른 결제 정보와 연관된 구독 정보가 정상적으로 존재할 때")
        class Context_with_valid_payment_and_subscription {

            @Test
            @DisplayName("결제 이력의 cancel() 메서드를 작동시키고, 연관된 구독 엔티티의 cancel()도 동시 작동시킨다.")
            void it_cancels_both_payment_history_and_subscription() {
                // given
                PaymentHistory mockHistory = mock(PaymentHistory.class);
                Subscription mockSubscription = mock(Subscription.class);

                when(mockHistory.getSubscription()).thenReturn(mockSubscription);
                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.of(mockHistory));

                // when
                paymentHistoryService.cancelPaymentAndSubscription(PAYMENT_HISTORY_ID, MEMBER_ID);

                // then
                // 결제 이력 엔티티 자체의 취소 도메인 로직이 실행되었는지 검증
                verify(mockHistory, times(1)).cancel();
                // 연관 구독 정보 엔티티 자체의 취소 도메인 로직이 실행되었는지 검증
                verify(mockSubscription, times(1)).cancel();
            }
        }

        @Nested
        @DisplayName("결제 정보는 유효하나 연관된 구독(Subscription) 엔티티 정보가 없다면 (null)")
        class Context_with_valid_payment_but_null_subscription {

            @Test
            @DisplayName("결제 이력만 취소 상태로 바꾸고, 구독 취소 로직은 건너뛴 뒤 정상 종료한다.")
            void it_cancels_only_payment_history_safely() {
                // given
                PaymentHistory mockHistory = mock(PaymentHistory.class);

                // 연관관계 맵핑이 null인 상황 재현 (if (subscription != null) 조건문 내부 분기 커버)
                when(mockHistory.getSubscription()).thenReturn(null);
                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.of(mockHistory));

                // when
                paymentHistoryService.cancelPaymentAndSubscription(PAYMENT_HISTORY_ID, MEMBER_ID);

                // then
                verify(mockHistory, times(1)).cancel();
                // NullPointerException 방지 및 구독 차단 검증 수행
                verify(mockHistory, times(1)).getSubscription();
            }
        }

        @Nested
        @DisplayName("취소 반영 단계에서 대상 결제 이력이 DB에서 조회되지 않는다면")
        class Context_with_non_existent_payment_history_in_cancel_step {

            @Test
            @DisplayName("PAYMENT_HISTORY_NOT_FOUND 에러 예외를 발생시키며 영속성 상태 변경을 전면 중단한다.")
            void it_throws_payment_history_not_found_exception() {
                // given
                when(paymentHistoryRepository.findByIdAndMemberId(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> paymentHistoryService.cancelPaymentAndSubscription(PAYMENT_HISTORY_ID, MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_HISTORY_NOT_FOUND);
            }
        }
    }
}