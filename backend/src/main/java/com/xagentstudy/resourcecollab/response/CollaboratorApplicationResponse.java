package com.xagentstudy.resourcecollab.response;

import com.xagentstudy.resourcecollab.CollaboratorApplication;

import java.time.OffsetDateTime;

public record CollaboratorApplicationResponse(
        Long id,
        Long userId,
        String reason,
        String expertise,
        String status,
        String reviewNote,
        Long reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CollaboratorApplicationResponse from(CollaboratorApplication application) {
        return new CollaboratorApplicationResponse(
                application.getId(),
                application.getUserId(),
                application.getReason(),
                application.getExpertise(),
                application.getStatus(),
                application.getReviewNote(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
