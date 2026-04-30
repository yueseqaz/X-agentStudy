package com.xagentstudy.profile;

import jakarta.validation.constraints.NotBlank;

public record ProfileAnswer(
        @NotBlank String question,
        @NotBlank String answer
) {
}
