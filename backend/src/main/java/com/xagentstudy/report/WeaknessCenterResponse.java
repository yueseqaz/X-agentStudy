package com.xagentstudy.report;

import java.util.List;

public record WeaknessCenterResponse(
        Long planId,
        WeaknessSummaryResponse summary,
        List<WeaknessItemResponse> items,
        List<String> suggestions
) {
}
