package come.back.gotoday.payment.history.service;

import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.history.dto.PaymentHistoryResponse;
import come.back.gotoday.payment.history.entity.PaymentHistory;
import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.history.enums.PaymentStatus;
import come.back.gotoday.payment.history.repository.PaymentHistoryRepository;
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

        if (history.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_FAILED_PAYMENT); // "실패한 결제는 취소할 수 없습니다." 관련 에러
        }

        return history.getPaymentKey();
    }

    /**
     * 외부 API 성공 후, 내부 DB 상태를 CANCELED로 최종 변경하는 단계
     */
    @Transactional
    public void cancelPaymentAndSubscription(Long paymentHistoryId, Long memberId) {
        // 1. 단 한 번의 조회로 영속성 컨텍스트에 로드 (페치 조인을 쓰면 성능이 더 올라갑니다)
        PaymentHistory history = paymentHistoryRepository.findByIdAndMemberId(paymentHistoryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        // 2. 결제 상태를 CANCELED로 변경
        history.cancel();

        // 3. 연관된 구독 해지 (동일 트랜잭션 내에서 안전하게 처리)
        Subscription subscription = history.getSubscription();
        if (subscription != null) {
            subscription.cancel();
        }
    }
}