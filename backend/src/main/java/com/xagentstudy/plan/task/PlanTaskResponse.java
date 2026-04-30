package com.xagentstudy.plan.task;

import java.time.OffsetDateTime;

public record PlanTaskResponse(
        Long id,
        Long planId,
        Integer stageIndex,
        Integer taskIndex,
        String taskText,
        Boolean completed,
        OffsetDateTime completedAt
) {
    public static PlanTaskResponse from(PlanTaskRecord record) {
        return new PlanTaskResponse(
                record.getId(),
                record.getPlanId(),
                record.getStageIndex(),
                record.getTaskIndex(),
                record.getTaskText(),
                record.getCompleted(),
                record.getCompletedAt()
        );
    }
}
