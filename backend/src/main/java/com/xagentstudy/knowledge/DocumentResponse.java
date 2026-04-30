package com.xagentstudy.knowledge;

import java.time.OffsetDateTime;

public record DocumentResponse(
        Long id,
        Long planId,
        String name,
        String type,
        String parseStatus,
        String summary,
        String sectionsJson,
        String keyPointsJson,
        String knowledgeTreeJson,
        Integer contentLength,
        String parseError,
        String learningStatus,
        OffsetDateTime uploadedAt
) {
    public static DocumentResponse from(KnowledgeDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getPlanId(),
                document.getName(),
                document.getType(),
                document.getParseStatus(),
                document.getSummary(),
                document.getSectionsJson(),
                document.getKeyPointsJson(),
                document.getKnowledgeTreeJson(),
                document.getContentLength(),
                document.getParseError(),
                document.getLearningStatus(),
                document.getUploadedAt()
        );
    }
}
