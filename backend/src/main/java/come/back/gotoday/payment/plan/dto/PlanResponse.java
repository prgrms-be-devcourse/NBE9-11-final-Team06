package come.back.gotoday.payment.plan.dto;

import come.back.gotoday.payment.plan.entity.Plan;

public record PlanResponse(
        Long id,
        String name,
        String displayName,
        Long amount
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDisplayName(),
                plan.getAmount()
        );
    }
}