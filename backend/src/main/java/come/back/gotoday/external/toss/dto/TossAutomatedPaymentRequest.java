package come.back.gotoday.external.toss.dto;

import lombok.Builder;

@Builder
public record TossAutomatedPaymentRequest(
        String customerKey,
        Long amount,
        String orderId,
        String orderName,
        String customerEmail,
        String customerName,
        Long taxFreeAmount,
        Long taxExemptionAmount
) {
    // 필수 값이 누락되지 않도록 빌더의 기본값 처리를 보완하는 컴팩트 생성자
    public TossAutomatedPaymentRequest {
        if (taxFreeAmount == null) taxFreeAmount = 0L;
        if (taxExemptionAmount == null) taxExemptionAmount = 0L;
    }
}