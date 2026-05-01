package com.xagentstudy.resourcecollab.response;

import com.xagentstudy.resourcecollab.IngestionSource;

import java.time.OffsetDateTime;

public record IngestionSourceResponse(
        Long id,
        Long ownerUserId,
        String name,
        String sourceType,
        String baseUrl,
        Boolean enabled,
        String sourceCategory,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static IngestionSourceResponse from(IngestionSource source) {
        return new IngestionSourceResponse(
                source.getId(),
                source.getOwnerUserId(),
                source.getName(),
                source.getSourceType(),
                source.getBaseUrl(),
                source.getEnabled(),
                source.getSourceCategory(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
