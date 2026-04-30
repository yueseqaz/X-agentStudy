package com.xagentstudy.dashboard;

public record DashboardSummaryResponse(
        int planCount,
        long documentCount,
        long questionCount,
        long pendingReviewCount
) {
}
