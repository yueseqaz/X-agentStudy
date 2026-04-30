package com.xagentstudy.plan;

import com.xagentstudy.direction.LearningDirection;

import java.time.OffsetDateTime;

public record PlanSummaryResponse(
        Long id,
        Long directionId,
        String directionName,
        String title,
        String status,
        OffsetDateTime createdAt
) {
    public static PlanSummaryResponse from(LearningPlan plan, LearningDirection direction) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getDirectionId(),
                direction.getName(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getCreatedAt()
        );
    }
}
