package come.back.gotoday.payment.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionPaymentCancelRequest(
        @NotBlank(message = "취소 사유는 필수 입력 사항입니다.")
        String cancelReason
) {}
