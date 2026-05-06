package com.xagentstudy.assessment;

public record StageAssessmentChallengeResponse(
        String id,
        String title,
        String prompt,
        String rubric,
        int estimatedMinutes
) {
}
