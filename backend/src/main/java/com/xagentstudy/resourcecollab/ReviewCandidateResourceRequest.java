package com.xagentstudy.resourcecollab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCandidateResourceRequest(
        @NotBlank @Size(max = 32) String status,
        @Size(max = 4000) String reviewNote
) {
}
