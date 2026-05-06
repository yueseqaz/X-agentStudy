package com.xagentstudy.assessment;

import java.util.List;

public record StageAssessmentResponse(
        Long planId,
        int stageIndex,
        String stageName,
        String stageFocus,
        int readinessScore,
        String readinessLabel,
        List<String> knowledgePoints,
        List<String> weakPoints,
        List<StageAssessmentChallengeResponse> challenges
) {
}
