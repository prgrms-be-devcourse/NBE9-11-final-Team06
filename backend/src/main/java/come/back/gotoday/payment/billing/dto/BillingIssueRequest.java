package come.back.gotoday.payment.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record BillingIssueRequest(
        @NotBlank(message = "authKey는 필수입니다.")
        String authKey,

        @NotBlank(message = "customerKey는 필수입니다.")
        String customerKey
) {}