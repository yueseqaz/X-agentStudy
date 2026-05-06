package com.xagentstudy.assessment;

import java.util.List;

public record StageAssessmentResultResponse(
        Long planId,
        int stageIndex,
        String stageName,
        int totalScore,
        boolean passed,
        String level,
        String conclusion,
        String passCardTitle,
        List<StageAssessmentItemResultResponse> results,
        List<String> nextActions
) {
}
