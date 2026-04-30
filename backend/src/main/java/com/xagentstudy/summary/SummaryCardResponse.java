package com.xagentstudy.summary;

import java.time.OffsetDateTime;

public record SummaryCardResponse(
        Long id,
        Long userId,
        Long planId,
        Long documentId,
        String title,
        String summary,
        String sourceDocumentName,
        String imageData,
        String contentJson,
        String templateType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SummaryCardResponse from(SummaryCard card) {
        return new SummaryCardResponse(
                card.getId(),
                card.getUserId(),
                card.getPlanId(),
                card.getDocumentId(),
                card.getTitle(),
                card.getSummary(),
                card.getSourceDocumentName(),
                card.getImageData(),
                card.getContentJson(),
                card.getTemplateType(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
