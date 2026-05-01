package com.xagentstudy.resourcecollab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIngestionSourceRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 32) String sourceType,
        @NotBlank @Size(max = 512) String baseUrl,
        @Size(max = 64) String sourceCategory
) {
}
