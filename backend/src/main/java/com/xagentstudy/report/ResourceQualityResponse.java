package com.xagentstudy.report;

import java.util.List;

public record ResourceQualityResponse(
        Long planId,
        int overallScore,
        String label,
        List<ResourceQualityItemResponse> items,
        List<String> suggestions
) {
}
