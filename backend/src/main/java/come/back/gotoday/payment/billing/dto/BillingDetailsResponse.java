package come.back.gotoday.payment.billing.dto;

import come.back.gotoday.payment.billing.entity.BillingInfo;
import java.time.LocalDateTime;

public record BillingDetailsResponse(
        Long id,
        String cardCompany,
        String cardNumber,
        LocalDateTime createdAt
) {
    public static BillingDetailsResponse from(BillingInfo billingInfo) {
        return new BillingDetailsResponse(
                billingInfo.getId(),
                billingInfo.getCardCompany(),
                billingInfo.getCardNumber(),
                billingInfo.getCreatedAt()
        );
    }
}