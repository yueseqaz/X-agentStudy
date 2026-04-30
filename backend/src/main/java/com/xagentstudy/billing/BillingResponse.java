package com.xagentstudy.billing;

import java.time.OffsetDateTime;

public record BillingResponse(
        String planCode,
        String planName,
        Integer monthlyPriceCents,
        Integer monthlyAgentQuota,
        Integer usedAgentCalls,
        Integer remainingAgentCalls,
        Integer storageQuotaMb,
        Integer usedStorageMb,
        Integer planQuota,
        Integer usedPlanCount,
        Integer walletBalanceCents,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd
) {
    public static BillingResponse from(SubscriptionAccount account) {
        return from(account, account.getUsedStorageMb(), 0);
    }

    public static BillingResponse from(SubscriptionAccount account, Integer usedStorageMb, Integer usedPlanCount) {
        SubscriptionPlan plan = SubscriptionPlan.fromCode(account.getPlanCode());
        return new BillingResponse(
                account.getPlanCode(),
                plan.displayName(),
                plan.monthlyPriceCents(),
                account.getMonthlyAgentQuota(),
                account.getUsedAgentCalls(),
                Math.max(0, account.getMonthlyAgentQuota() - account.getUsedAgentCalls()),
                account.getStorageQuotaMb(),
                usedStorageMb == null ? 0 : usedStorageMb,
                plan.planQuota(),
                usedPlanCount == null ? 0 : usedPlanCount,
                account.getWalletBalanceCents(),
                account.getPeriodStart(),
                account.getPeriodEnd()
        );
    }
}
