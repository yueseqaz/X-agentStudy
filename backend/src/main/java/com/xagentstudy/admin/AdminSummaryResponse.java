package com.xagentstudy.admin;

public record AdminSummaryResponse(
        long users,
        long directions,
        long plans,
        long documents,
        long questions,
        long tasks,
        long runningTasks,
        long failedTasks,
        long subscriptions,
        long disabledUsers,
        long lockedUsers,
        long quotaUsed,
        long quotaTotal
) {
}
