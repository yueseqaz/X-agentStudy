package com.xagentstudy.plan;

import java.time.OffsetDateTime;

public record PlanResponse(
        Long id,
        Long directionId,
        String directionName,
        String directionCategory,
        Long profileId,
        String title,
        String status,
        String goal,
        String stages,
        Integer currentStageIndex,
        OffsetDateTime createdAt
) {
    public static PlanResponse from(LearningPlan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getDirectionId(),
                null,
                null,
                plan.getProfileId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getGoal(),
                plan.getStages(),
                plan.getCurrentStageIndex(),
                plan.getCreatedAt()
        );
    }

    public static PlanResponse from(LearningPlan plan, com.xagentstudy.direction.LearningDirection direction) {
        return new PlanResponse(
                plan.getId(),
                plan.getDirectionId(),
                direction.getName(),
                direction.getCategory(),
                plan.getProfileId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getGoal(),
                plan.getStages(),
                plan.getCurrentStageIndex(),
                plan.getCreatedAt()
        );
    }
}
