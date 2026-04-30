package com.xagentstudy.review;

import java.util.List;

public record ReviewSummaryResponse(
        Long planId,
        int totalCount,
        int pendingCount,
        int completedCount,
        List<ReviewRecordResponse> records
) {
}
