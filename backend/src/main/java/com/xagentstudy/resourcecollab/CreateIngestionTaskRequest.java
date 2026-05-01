package com.xagentstudy.resourcecollab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIngestionTaskRequest(
        @NotNull Long sourceId,
        @NotBlank @Size(max = 512) String targetUrl,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String summary,
        @Size(max = 512) String tags,
        @Size(max = 255) String authorName,
        @Size(max = 512) String coverImageUrl,
        Integer durationSeconds,
        String rawContent
) {
}
