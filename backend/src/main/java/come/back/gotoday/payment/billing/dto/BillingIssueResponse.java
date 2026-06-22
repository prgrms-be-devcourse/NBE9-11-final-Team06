package come.back.gotoday.payment.billing.dto;

import come.back.gotoday.payment.billing.entity.BillingInfo;

public record BillingIssueResponse(
        Long billingInfoId,
        String cardCompany,
        String cardNumber
) {
    public static BillingIssueResponse from(BillingInfo billingInfo) {
        return new BillingIssueResponse(
                billingInfo.getId(),
                billingInfo.getCardCompany(),
                billingInfo.getCardNumber()
        );
    }
}