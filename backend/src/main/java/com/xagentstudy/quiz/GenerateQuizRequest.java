package com.xagentstudy.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateQuizRequest(
        @Min(3) @Max(20) Integer count,
        String difficulty,
        String questionType,
        Long documentId,
        String knowledgePoint
) {
    public int safeCount() {
        if (count == null) {
            return 3;
        }
        return Math.max(3, Math.min(20, count));
    }

    public String safeDifficulty() {
        return difficulty == null || difficulty.isBlank() ? "MEDIUM" : difficulty;
    }

    public String safeQuestionType() {
        return questionType == null || questionType.isBlank() ? "SINGLE_CHOICE" : questionType;
    }
}
