package come.back.gotoday.payment.subscription.dto;

import jakarta.validation.constraints.NotNull;

public record SubscriptionRequest(
        @NotNull(message = "결제 수단 정보는 필수입니다.")
        Long billingInfoId,

        @NotNull(message = "구독할 요금제 번호는 필수입니다.")
        Long planId
) {}