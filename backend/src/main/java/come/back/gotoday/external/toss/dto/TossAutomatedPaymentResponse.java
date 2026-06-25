package come.back.gotoday.external.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossAutomatedPaymentResponse(
        String mId,
        String lastTransactionKey,
        String paymentKey,
        String orderId,
        String orderName,
        String status, // DONE, READY, CANCELED 등
        String requestedAt,
        String approvedAt,
        String type, // BILLING
        String method, // 카드
        Long totalAmount,
        CardInfo card,
        ReceiptInfo receipt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardInfo(
            String issuerCode,
            String acquirerCode,
            String number, // 마스킹된 카드번호
            Long amount,
            String cardType,
            String ownerType,
            String approveNo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReceiptInfo(
            String url // 전표 영수증 URL
    ) {}

    // null 안전하게 영수증 URL을 꺼내기 위한 편의 메서드
    public String getReceiptUrl() {
        return (receipt != null) ? receipt.url() : null;
    }
}