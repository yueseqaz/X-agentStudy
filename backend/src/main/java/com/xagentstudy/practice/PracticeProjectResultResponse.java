package com.xagentstudy.practice;

import java.util.List;

public record PracticeProjectResultResponse(
        Long planId,
        int stageIndex,
        String stageName,
        int totalScore,
        boolean passed,
        String level,
        String outcomeCardTitle,
        String conclusion,
        List<String> coveredKnowledgePoints,
        List<String> unstableKnowledgePoints,
        List<PracticeProjectTaskResultResponse> taskResults,
        List<String> nextActions
) {
}
