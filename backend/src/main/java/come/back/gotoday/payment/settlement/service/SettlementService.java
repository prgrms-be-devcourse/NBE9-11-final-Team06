package come.back.gotoday.payment.settlement.service;

import come.back.gotoday.external.toss.dto.SettlementDto;
import come.back.gotoday.payment.settlement.entity.SettlementDetail;
import come.back.gotoday.payment.settlement.enums.SettlementStatus;
import come.back.gotoday.payment.settlement.repository.SettlementDetailRepository;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository; // 가정된 레포지토리
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SettlementDetailRepository settlementDetailRepository;

    /**
     * 토스 정산 데이터를 받아와 우리 DB 장부와 대조 작업을 수행합니다.
     */
    public void reconcileSettlement(List<SettlementDto.TossSettlementResponse> tossResponses) {
        for (SettlementDto.TossSettlementResponse toss : tossResponses) {
            boolean isAlreadyProcessed = settlementDetailRepository.existsByOrderIdAndAmount(toss.orderId(), toss.amount());
            if (isAlreadyProcessed) {
                continue; // 이미 며칠 전 배치에서 처리한 데이터이므로 중복 대조하지 않고 패스!
            }
            // 1. 우리 DB에서 orderId로 결제 이력 조회
            Optional<PaymentHistory> historyOpt = paymentHistoryRepository.findByOrderId(toss.orderId());

            // 케이스 1: 우리 DB에 결제 이력 자체가 없는 경우
            if (historyOpt.isEmpty()) {
                log.error("[정산 에러] 우리 DB에 존재하지 않는 주문 ID 정산 요청 발생. OrderId: {}", toss.orderId());
                saveMismatchedDetail(null, toss, SettlementStatus.NOT_FOUND_PAYMENT, "우리 DB에 해당 주문 ID 정보가 없음");
                continue;
            }

            PaymentHistory paymentHistory = historyOpt.get();

            // 2. 결제 상태 검증 (토스 데이터가 취소건인지 일반 결제건인지 판별)
            boolean isTossCancel = toss.cancel() != null || toss.amount() < 0;

            if (isTossCancel) {
                // 케이스 2-A: 토스는 취소건인데 우리 DB는 여전히 SUCCESS인 경우 (상태 불일치)
                if (paymentHistory.getStatus() != PaymentStatus.CANCELED) {
                    processMismatch(paymentHistory, toss, SettlementStatus.MISMATCHED_STATUS, "토스는 취소 정산이나 우리 DB는 취소 상태가 아님");
                    continue;
                }

                // 케이스 2-B: 취소 금액 일치 여부 검증 (토스 금액 부호 반전하여 비교)
                long expectedCancelAmount = Math.abs(toss.amount());
                if (!paymentHistory.getAmount().equals(expectedCancelAmount)) {
                    processMismatch(paymentHistory, toss, SettlementStatus.MISMATCHED_AMOUNT, "취소 정산 금액 불일치. DB: " + paymentHistory.getAmount() + ", 토스: " + expectedCancelAmount);
                    continue;
                }

            } else {
                // 케이스 3-A: 토스는 정상 결제인데 우리 DB는 SUCCESS가 아닌 경우 (상태 불일치)
                if (paymentHistory.getStatus() != PaymentStatus.SUCCESS) {
                    processMismatch(paymentHistory, toss, SettlementStatus.MISMATCHED_STATUS, "토스는 정상 결제 정산이나 우리 DB 상태가 SUCCESS가 아님");
                    continue;
                }

                // 케이스 3-B: 정상 결제 금액 일치 여부 검증
                if (!paymentHistory.getAmount().equals(toss.amount())) {
                    processMismatch(paymentHistory, toss, SettlementStatus.MISMATCHED_AMOUNT, "결제 정산 금액 불일치. DB: " + paymentHistory.getAmount() + ", 토스: " + toss.amount());
                    continue;
                }
            }

            // 케이스 4: 모든 검증 통과 - 정상 장부 기록 (MATCHED)
            SettlementDetail matchedDetail = SettlementDetail.create(
                    paymentHistory, toss.orderId(), toss.paymentKey(), toss.amount(),
                    toss.fee(), toss.vat(), toss.payOutAmount(), toss.paidOutDate(),
                    SettlementStatus.MATCHED, "정상 대조 완료"
            );
            settlementDetailRepository.save(matchedDetail);
        }
    }

    private void processMismatch(PaymentHistory history, SettlementDto.TossSettlementResponse toss, SettlementStatus status, String description) {
        log.warn("[정산 불일치 발생] 상태: {}, 사유: {}, OrderId: {}", status, description, toss.orderId());

        // 기존 구독 엔티티의 관리 상태도 변경 (필요시 시스템 자동 결제 방어용)
        history.getSubscription().changeToManualCheck();

        // 정산 상세 테이블에 불일치 원인 기록
        saveMismatchedDetail(history, toss, status, description);

        // TODO: 사내 메신저(Slack Webhook 등) 알림 발송 공통 로직
    }

    private void saveMismatchedDetail(PaymentHistory history, SettlementDto.TossSettlementResponse toss, SettlementStatus status, String description) {
        SettlementDetail mismatchedDetail = SettlementDetail.create(
                history, toss.orderId(), toss.paymentKey(), toss.amount(),
                toss.fee(), toss.vat(), toss.payOutAmount(), toss.paidOutDate(),
                status, description
        );
        settlementDetailRepository.save(mismatchedDetail);
    }
}