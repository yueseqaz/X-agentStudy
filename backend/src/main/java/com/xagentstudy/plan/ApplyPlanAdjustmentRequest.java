package com.xagentstudy.plan;

import java.util.List;

public record ApplyPlanAdjustmentRequest(
        String stageName,
        List<String> tasks
) {
}
