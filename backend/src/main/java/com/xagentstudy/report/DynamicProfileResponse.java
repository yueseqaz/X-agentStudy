package com.xagentstudy.report;

import java.util.List;

public record DynamicProfileResponse(
        Long planId,
        int completenessScore,
        String summary,
        List<DynamicProfileDimensionResponse> dimensions,
        List<DynamicProfileSignalResponse> updateSignals,
        List<DynamicProfileRecommendationResponse> recommendationReasons
) {
}
