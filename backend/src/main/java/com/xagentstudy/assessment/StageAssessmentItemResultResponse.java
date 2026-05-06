package com.xagentstudy.assessment;

public record StageAssessmentItemResultResponse(
        String challengeId,
        String title,
        int score,
        String feedback
) {
}
