package com.xagentstudy.modelconfig;

import jakarta.validation.constraints.NotBlank;

public record SaveModelConfigRequest(
        @NotBlank String provider,
        @NotBlank String modelName,
        String baseUrl,
        String apiKey
) {
}
