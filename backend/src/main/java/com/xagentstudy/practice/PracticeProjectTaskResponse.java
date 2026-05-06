package com.xagentstudy.practice;

import java.util.List;

public record PracticeProjectTaskResponse(
        String id,
        String title,
        String description,
        List<String> knowledgePoints,
        String acceptanceCriteria,
        int estimatedMinutes
) {
}
