package come.back.gotoday.external.toss.webhook.dto;

public record TossBillingWebhookRequest(
        String eventType,
        String createdAt,
        BillingData data
) implements TossWebhookRequest {

    public record BillingData(
            String billingKey,
            String reason
    ) {}
}