package come.back.gotoday.external.toss.webhook.dto;


import java.time.OffsetDateTime;

public record TossPaymentWebhookRequest(
        String eventType,
        String createdAt,
        PaymentData data
) implements TossWebhookRequest {

    public record PaymentData(
            String orderId,
            String paymentKey,
            String status,
            Long totalAmount,
            String method,
            OffsetDateTime approvedAt,
            String receiptUrl
    ) {}
}