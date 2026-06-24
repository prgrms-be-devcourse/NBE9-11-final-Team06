package come.back.gotoday.payment.subscription.dto;

import come.back.gotoday.payment.subscription.entity.Subscription;
import come.back.gotoday.payment.subscription.enums.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record SubscriptionResponse(
        Long subscriptionId,
        String planName,
        Long amount,
        LocalDate nextBillingDate,
        SubscriptionStatus status
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .planName(subscription.getPlan().getDisplayName())
                .amount(subscription.getAmount()) // 변경에 유연한 스냅샷 금액 사용
                .nextBillingDate(subscription.getNextBillingDate())
                .status(subscription.getStatus())
                .build();
    }
}