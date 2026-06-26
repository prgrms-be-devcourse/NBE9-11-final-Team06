package come.back.gotoday.external.toss.webhook.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType", // 🌟 이 필드값으로 어떤 DTO로 파싱할지 결정합니다.
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TossPaymentWebhookRequest.class, name = "PAYMENT_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = TossBillingWebhookRequest.class, name = "BILLING_DELETED")
})
public interface TossWebhookRequest {
    String eventType();
    String createdAt();
}