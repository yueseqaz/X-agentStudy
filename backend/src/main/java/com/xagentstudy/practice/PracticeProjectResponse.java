package com.xagentstudy.practice;

import java.util.List;

public record PracticeProjectResponse(
        Long planId,
        int stageIndex,
        String stageName,
        String title,
        String scenario,
        List<String> deliverables,
        List<String> knowledgePoints,
        List<String> weakPoints,
        List<PracticeProjectTaskResponse> tasks
) {
}
