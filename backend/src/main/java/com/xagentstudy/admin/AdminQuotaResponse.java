package com.xagentstudy.admin;

import com.xagentstudy.billing.SubscriptionAccount;
import com.xagentstudy.billing.SubscriptionPlan;

public record AdminQuotaResponse(
        Long userId,
        String nickname,
        String account,
        String planCode,
        Integer monthlyAgentQuota,
        Integer usedAgentCalls,
        Integer remainingAgentCalls,
        Integer storageQuotaMb,
        Integer usedStorageMb,
        Integer planQuota,
        Integer usedPlanCount,
        Integer walletBalanceCents,
        double usageRate
) {
    public static AdminQuotaResponse from(SubscriptionAccount account) {
        return from(account, null, account.getUsedStorageMb(), 0);
    }

    public static AdminQuotaResponse from(SubscriptionAccount account, com.xagentstudy.user.AppUser user, Integer usedStorageMb, Integer usedPlanCount) {
        int remaining = Math.max(0, account.getMonthlyAgentQuota() - account.getUsedAgentCalls());
        double usageRate = account.getMonthlyAgentQuota() == 0
                ? 0
                : Math.round(account.getUsedAgentCalls() * 1000.0 / account.getMonthlyAgentQuota()) / 10.0;
        return new AdminQuotaResponse(
                account.getUserId(),
                user == null ? "-" : user.getNickname(),
                user == null ? "-" : user.getAccount(),
                account.getPlanCode(),
                account.getMonthlyAgentQuota(),
                account.getUsedAgentCalls(),
                remaining,
                account.getStorageQuotaMb(),
                usedStorageMb == null ? 0 : usedStorageMb,
                SubscriptionPlan.fromCode(account.getPlanCode()).planQuota(),
                usedPlanCount == null ? 0 : usedPlanCount,
                account.getWalletBalanceCents(),
                usageRate
        );
    }
}
