package come.back.gotoday.payment.history.service;

import come.back.gotoday.external.toss.TossPaymentsClient;
import come.back.gotoday.external.toss.dto.TossCancelRequest;
import come.back.gotoday.external.toss.dto.TossCancelResponse;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import come.back.gotoday.payment.subscription.dto.SubscriptionPaymentCancelRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentHistoryFacade {

    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentHistoryService paymentHistoryService;

    /**
     * 외부 API 연동을 트랜잭션 외부에서 실행하여 결제 취소 처리
     */
    public void cancelPayment(Long memberId, Long paymentHistoryId, SubscriptionPaymentCancelRequest request) {
        // 1. 내부 DB에서 사용자의 결제 이력이 맞는지 검증 및 평문 paymentKey 추출
        String paymentKey = paymentHistoryService.getPaymentKeyValidated(paymentHistoryId, memberId);

        // 2. [트랜잭션 외부 Network I/O] 토스페이먼츠 결제 취소 API 호출
        TossCancelRequest tossCancelRequest = new TossCancelRequest(request.cancelReason());
        TossCancelResponse tossResponse = tossPaymentsClient.cancelPayment(paymentKey, tossCancelRequest);

        // 3. 외부 취소 성공 시 내부 DB 상태 변경 및 반영
        if (tossResponse != null &&"CANCELED".equals(tossResponse.status())) {
            paymentHistoryService.cancelPaymentAndSubscription(paymentHistoryId, memberId);
        } else {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}