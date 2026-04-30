package com.xagentstudy.billing;

import java.util.List;

public record SubscriptionPlanResponse(
        String code,
        String displayName,
        int monthlyPriceCents,
        int monthlyAgentQuota,
        int storageQuotaMb,
        int planQuota,
        List<String> features
) {
    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.name(),
                plan.displayName(),
                plan.monthlyPriceCents(),
                plan.monthlyAgentQuota(),
                plan.storageQuotaMb(),
                plan.planQuota(),
                plan.features()
        );
    }
}
