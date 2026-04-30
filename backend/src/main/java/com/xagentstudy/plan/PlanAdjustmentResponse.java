package com.xagentstudy.plan;

import java.util.List;

public record PlanAdjustmentResponse(
        Long planId,
        double taskCompletionRate,
        double accuracyRate,
        List<String> risks,
        List<String> adjustments,
        List<String> nextActions
) {
}
