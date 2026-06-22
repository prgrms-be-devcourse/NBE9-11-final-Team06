package come.back.gotoday.payment.billing.dto;

public record TossBillingKeyResponse(
        String mId,
        String customerKey,
        String billingKey,
        String authenticatedAt,
        String method,
        CardInfo card
) {
    public record CardInfo(
            String issuerCode,
            String acquirerCode,
            String number,
            String cardType,
            String ownerType,
            String company
    ) {}
}