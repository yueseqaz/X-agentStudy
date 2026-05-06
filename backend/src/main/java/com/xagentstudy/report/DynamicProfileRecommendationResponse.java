package com.xagentstudy.report;

public record DynamicProfileRecommendationResponse(
        String target,
        String reason,
        String evidence
) {
}
