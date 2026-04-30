package com.xagentstudy.modelconfig;

import java.time.OffsetDateTime;

public record ModelConfigResponse(
        Long id,
        String provider,
        String modelName,
        String baseUrl,
        String apiKeyMask,
        Boolean enabled,
        OffsetDateTime updatedAt
) {
    public static ModelConfigResponse from(ModelConfig config) {
        return new ModelConfigResponse(
                config.getId(),
                config.getProvider(),
                config.getModelName(),
                config.getBaseUrl(),
                config.getApiKeyMask(),
                config.getEnabled(),
                config.getUpdatedAt()
        );
    }
}
