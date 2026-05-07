package com.xagentstudy.plan;

import java.time.OffsetDateTime;

public record PublicPlanResponse(
        String shareCode,
        String title,
        String directionName,
        String directionCategory,
        String goal,
        String stages,
        OffsetDateTime createdAt
) {
    public static PublicPlanResponse from(LearningPlan plan, com.xagentstudy.direction.LearningDirection direction) {
        return new PublicPlanResponse(
                plan.getShareCode(),
                plan.getTitle(),
                direction.getName(),
                direction.getCategory(),
                plan.getGoal(),
                plan.getStages(),
                plan.getCreatedAt()
        );
    }
}
