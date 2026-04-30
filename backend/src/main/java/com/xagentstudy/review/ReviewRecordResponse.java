package com.xagentstudy.review;

import java.time.OffsetDateTime;

public record ReviewRecordResponse(
        Long id,
        Long planId,
        String knowledgePoint,
        String reason,
        Integer priorityLevel,
        Boolean completed,
        Integer masteryScore,
        Integer intervalDays,
        OffsetDateTime dueAt,
        OffsetDateTime recommendedAt,
        OffsetDateTime completedAt
) {
    public static ReviewRecordResponse from(ReviewRecord record) {
        return new ReviewRecordResponse(
                record.getId(),
                record.getPlanId(),
                record.getKnowledgePoint(),
                record.getReason(),
                record.getPriorityLevel(),
                record.getCompleted(),
                record.getMasteryScore(),
                record.getIntervalDays(),
                record.getDueAt(),
                record.getRecommendedAt(),
                record.getCompletedAt()
        );
    }
}
