package come.back.gotoday.payment.settlement.service;

import come.back.gotoday.external.toss.dto.SettlementDto;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
import come.back.gotoday.payment.settlement.entity.SettlementDetail;
import come.back.gotoday.payment.settlement.enums.SettlementStatus;
import come.back.gotoday.payment.settlement.repository.SettlementDetailRepository;
import come.back.gotoday.payment.subscription.entity.Subscription;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService 단위 테스트")
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private SettlementDetailRepository settlementDetailRepository;

    private final String ORDER_ID = "ord-2026-0629-001";
    private final String PAYMENT_KEY = "toss-payment-key-2026";
    private final LocalDate PAID_OUT_DATE = LocalDate.of(2026, 6, 29);

    private SettlementDto.TossSettlementResponse createTossResponse(long amount, SettlementDto.CancelInfo cancelInfo) {
        return new SettlementDto.TossSettlementResponse(
                ORDER_ID, PAYMENT_KEY, amount, 300L, 30L, amount - 330L, PAID_OUT_DATE, cancelInfo
        );
    }

    @Nested
    @DisplayName("reconcileSettlement 메서드는")
    class Describe_reconcileSettlement {

        @Test
        @DisplayName("이미 과거 배치에서 처리된 중복 정산 데이터라면 더 이상 진행하지 않고 패스한다.")
        void it_skips_when_already_processed() {
            // given
            SettlementDto.TossSettlementResponse toss = createTossResponse(9900L, null);
            when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, 9900L)).thenReturn(true);

            // when
            settlementService.reconcileSettlement(List.of(toss));

            // then
            verify(paymentHistoryRepository, never()).findByOrderId(anyString());
            verify(settlementDetailRepository, never()).save(any(SettlementDetail.class));
        }

        @Test
        @DisplayName("토스 정산 주문 ID가 우리 DB 장부에 아예 존재하지 않는다면 NOT_FOUND_PAYMENT 상세 내역을 남긴다.")
        void it_records_not_found_payment_when_history_is_missing() {
            // given
            SettlementDto.TossSettlementResponse toss = createTossResponse(9900L, null);
            when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, 9900L)).thenReturn(false);
            when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            // when
            settlementService.reconcileSettlement(List.of(toss));

            // then
            verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                    detail.getStatus() == SettlementStatus.NOT_FOUND_PAYMENT &&
                            detail.getOrderId().equals(ORDER_ID)
            ));
        }

        @Nested
        @DisplayName("토스 데이터가 [취소 정산건]일 때")
        class Context_with_toss_cancel_response {

            @Test
            @DisplayName("우리 DB 결제 상태가 CANCELED가 아니라면 MISMATCHED_STATUS 정보를 기록하고 구독을 수동 검사로 전환한다.")
            void it_records_mismatched_status_when_our_db_is_not_canceled() {
                // given
                // 실제 CancelInfo 객체를 조립하여 주입 (금액 음수 처리)
                SettlementDto.CancelInfo cancelInfo = new SettlementDto.CancelInfo("고객 변심", 9900L, "DONE");
                SettlementDto.TossSettlementResponse tossCancel = createTossResponse(-9900L, cancelInfo);

                PaymentHistory mockHistory = mock(PaymentHistory.class);
                Subscription mockSubscription = mock(Subscription.class);

                when(mockHistory.getStatus()).thenReturn(PaymentStatus.SUCCESS); // 🚨 불일치 발생 (SUCCESS vs 취소건)
                when(mockHistory.getSubscription()).thenReturn(mockSubscription);

                when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, -9900L)).thenReturn(false);
                when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(mockHistory));

                // when
                settlementService.reconcileSettlement(List.of(tossCancel));

                // then
                verify(mockSubscription, times(1)).changeToManualCheck();
                verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                        detail.getStatus() == SettlementStatus.MISMATCHED_STATUS
                ));
            }

            @Test
            @DisplayName("상태는 CANCELED로 일치하나 취소 금액의 크기가 서로 다르면 MISMATCHED_AMOUNT 정보를 기록한다.")
            void it_records_mismatched_amount_when_cancel_amounts_do_not_match() {
                // given
                SettlementDto.CancelInfo cancelInfo = new SettlementDto.CancelInfo("부분 취소", 5000L, "DONE");
                SettlementDto.TossSettlementResponse tossCancel = createTossResponse(-5000L, cancelInfo); // 토스는 5000원 취소

                PaymentHistory mockHistory = mock(PaymentHistory.class);
                Subscription mockSubscription = mock(Subscription.class);

                when(mockHistory.getStatus()).thenReturn(PaymentStatus.CANCELED); // 상태는 일치
                when(mockHistory.getAmount()).thenReturn(9900L); // 🚨 금액 불일치 발생 (장부 9900원 vs 토스 절대값 5000원)
                when(mockHistory.getSubscription()).thenReturn(mockSubscription);

                when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, -5000L)).thenReturn(false);
                when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(mockHistory));

                // when
                settlementService.reconcileSettlement(List.of(tossCancel));

                // then
                verify(mockSubscription, times(1)).changeToManualCheck();
                verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                        detail.getStatus() == SettlementStatus.MISMATCHED_AMOUNT
                ));
            }
        }

        @Nested
        @DisplayName("토스 데이터가 [일반 정상 승인건]일 때")
        class Context_with_toss_success_response {

            @Test
            @DisplayName("우리 DB 결제 상태가 SUCCESS가 아니라면 MISMATCHED_STATUS 정보를 기록하고 구독을 수동 검사로 전환한다.")
            void it_records_mismatched_status_when_our_db_is_not_success() {
                // given
                SettlementDto.TossSettlementResponse tossSuccess = createTossResponse(9900L, null); // 정상 승인이므로 cancel은 null

                PaymentHistory mockHistory = mock(PaymentHistory.class);
                Subscription mockSubscription = mock(Subscription.class);

                when(mockHistory.getStatus()).thenReturn(PaymentStatus.FAILED); // 🚨 불일치 발생 (FAILED vs 승인건)
                when(mockHistory.getSubscription()).thenReturn(mockSubscription);

                when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, 9900L)).thenReturn(false);
                when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(mockHistory));

                // when
                settlementService.reconcileSettlement(List.of(tossSuccess));

                // then
                verify(mockSubscription, times(1)).changeToManualCheck();
                verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                        detail.getStatus() == SettlementStatus.MISMATCHED_STATUS
                ));
            }

            @Test
            @DisplayName("상태는 SUCCESS로 일치하나 정산 대상 승인 금액이 서로 다르면 MISMATCHED_AMOUNT 정보를 기록한다.")
            void it_records_mismatched_amount_when_success_amounts_do_not_match() {
                // given
                SettlementDto.TossSettlementResponse tossSuccess = createTossResponse(9900L, null);

                PaymentHistory mockHistory = mock(PaymentHistory.class);
                Subscription mockSubscription = mock(Subscription.class);

                when(mockHistory.getStatus()).thenReturn(PaymentStatus.SUCCESS); // 상태 일치
                when(mockHistory.getAmount()).thenReturn(5000L); // 🚨 금액 불일치 발생 (장부 5000원 vs 토스 9900원)
                when(mockHistory.getSubscription()).thenReturn(mockSubscription);

                when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, 9900L)).thenReturn(false);
                when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(mockHistory));

                // when
                settlementService.reconcileSettlement(List.of(tossSuccess));

                // then
                verify(mockSubscription, times(1)).changeToManualCheck();
                verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                        detail.getStatus() == SettlementStatus.MISMATCHED_AMOUNT
                ));
            }

            @Test
            @DisplayName("상태와 금액이 모두 완벽하게 일치하면 정상 대조 완료(MATCHED) 상태로 최종 정산 확정 내역을 기록한다.")
            void it_saves_matched_settlement_detail_safely() {
                // given
                SettlementDto.TossSettlementResponse tossSuccess = createTossResponse(9900L, null);

                PaymentHistory mockHistory = mock(PaymentHistory.class);

                when(mockHistory.getStatus()).thenReturn(PaymentStatus.SUCCESS); // 상태 일치
                when(mockHistory.getAmount()).thenReturn(9900L); // 금액 완벽 일치

                when(settlementDetailRepository.existsByOrderIdAndAmount(ORDER_ID, 9900L)).thenReturn(false);
                when(paymentHistoryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(mockHistory));

                // when
                settlementService.reconcileSettlement(List.of(tossSuccess));

                // then
                verify(mockHistory, never()).getSubscription(); // 정상 건이므로 경고용 상태 변경 미작동 검증
                verify(settlementDetailRepository, times(1)).save(argThat(detail ->
                        detail.getStatus() == SettlementStatus.MATCHED &&
                                detail.getDescription().contains("정상 대조 완료")
                ));
            }
        }
    }
}