package com.xagentstudy.quiz;

import jakarta.validation.constraints.NotBlank;

public record SubmitAnswerRequest(
        @NotBlank String answer,
        Long redoOfQuestionId
) {
}
