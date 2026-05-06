package com.xagentstudy.practice;

public record PracticeProjectTaskResultResponse(
        String taskId,
        String title,
        int score,
        String feedback
) {
}
