package com.xagentstudy.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityQuestionRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 255) String tags
) {
}
