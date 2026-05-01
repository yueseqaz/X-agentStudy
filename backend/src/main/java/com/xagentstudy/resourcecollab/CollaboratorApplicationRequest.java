package com.xagentstudy.resourcecollab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollaboratorApplicationRequest(
        @NotBlank @Size(max = 4000) String reason,
        @Size(max = 512) String expertise
) {
}
