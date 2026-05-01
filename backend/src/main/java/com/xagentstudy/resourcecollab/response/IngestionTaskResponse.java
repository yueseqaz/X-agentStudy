package com.xagentstudy.resourcecollab.response;

import com.xagentstudy.resourcecollab.IngestionTask;

import java.time.OffsetDateTime;

public record IngestionTaskResponse(
        Long id,
        Long ownerUserId,
        Long sourceId,
        String sourceType,
        String targetUrl,
        String titleHint,
        String summaryHint,
        String tags,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static IngestionTaskResponse from(IngestionTask task) {
        return new IngestionTaskResponse(
                task.getId(),
                task.getOwnerUserId(),
                task.getSourceId(),
                task.getSourceType(),
                task.getTargetUrl(),
                task.getTitleHint(),
                task.getSummaryHint(),
                task.getTags(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
