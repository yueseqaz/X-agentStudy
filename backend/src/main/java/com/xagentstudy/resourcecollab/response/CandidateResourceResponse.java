package com.xagentstudy.resourcecollab.response;

import com.xagentstudy.resourcecollab.CandidateResource;

import java.time.OffsetDateTime;

public record CandidateResourceResponse(
        Long id,
        Long ownerUserId,
        Long sourceId,
        Long taskId,
        String sourceType,
        String title,
        String summary,
        String resourceUrl,
        String authorName,
        String coverImageUrl,
        Integer durationSeconds,
        String tags,
        String reviewStatus,
        String contentCaptureMode,
        String rawContent,
        String reviewNote,
        Long reviewedBy,
        OffsetDateTime reviewedAt,
        Long publishedResourceId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CandidateResourceResponse from(CandidateResource resource) {
        return new CandidateResourceResponse(
                resource.getId(),
                resource.getOwnerUserId(),
                resource.getSourceId(),
                resource.getTaskId(),
                resource.getSourceType(),
                resource.getTitle(),
                resource.getSummary(),
                resource.getResourceUrl(),
                resource.getAuthorName(),
                resource.getCoverImageUrl(),
                resource.getDurationSeconds(),
                resource.getTags(),
                resource.getReviewStatus(),
                resource.getContentCaptureMode(),
                resource.getRawContent(),
                resource.getReviewNote(),
                resource.getReviewedBy(),
                resource.getReviewedAt(),
                resource.getPublishedResourceId(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
