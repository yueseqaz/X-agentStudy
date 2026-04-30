package com.xagentstudy.direction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDirectionRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String category,
        @Size(max = 2000) String description
) {
}
