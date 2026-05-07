package com.xagentstudy.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityAnswerRequest(
        @NotBlank @Size(max = 4000) String content
) {
}
