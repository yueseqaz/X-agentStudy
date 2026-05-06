package com.xagentstudy.dashboard;

import java.util.List;

public record LearningCoachResponse(
        Long planId,
        String planTitle,
        String directionName,
        String headline,
        String reason,
        Integer estimatedMinutes,
        String primaryActionLabel,
        String primaryActionPath,
        List<String> nextSteps,
        List<String> signals
) {
}
