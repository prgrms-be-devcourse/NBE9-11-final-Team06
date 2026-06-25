package come.back.gotoday.payment.subscription.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.subscription.dto.PaymentHistoryResponse;
import come.back.gotoday.payment.subscription.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    /**
     * 사용자의 결제 내역 전체 조회
     */
    public List<PaymentHistoryResponse> getPaymentHistories(Long memberId) {
        List<PaymentHistory> histories = paymentHistoryRepository.findAllByMemberId(memberId);
        return histories.stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    /**
     * 취소를 위한 토스 paymentKey 조회 및 검증 단계
     */
    public String getPaymentKeyValidated(Long paymentHistoryId, Long memberId) {
        PaymentHistory history = paymentHistoryRepository.findByIdAndMemberId(paymentHistoryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        if (history.getPaymentKey() == null) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_FAILED_PAYMENT); // "실패한 결제는 취소할 수 없습니다." 관련 에러
        }

        return history.getPaymentKey();
    }

    /**
     * 외부 API 성공 후, 내부 DB 상태를 CANCELED로 최종 변경하는 단계
     */
    @Transactional
    public void completeCancelPayment(Long paymentHistoryId, Long memberId) {
        PaymentHistory history = paymentHistoryRepository.findByIdAndMemberId(paymentHistoryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        history.cancel(); // 엔티티 내부에서 상태를 CANCELED로 변환
    }

    @Transactional
    public void cancelAssociatedSubscription(Long paymentHistoryId) {
        PaymentHistory paymentHistory = paymentHistoryRepository.findById(paymentHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        Subscription subscription = paymentHistory.getSubscription();

        if (subscription != null) {
            subscription.cancel();
        }
    }
}