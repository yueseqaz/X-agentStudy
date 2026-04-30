package com.xagentstudy.agent.task;

import java.time.OffsetDateTime;

public record AgentTaskResponse(
        Long id,
        Long planId,
        String taskType,
        String status,
        String inputPayload,
        String outputPayload,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AgentTaskResponse from(AgentTask task) {
        return new AgentTaskResponse(
                task.getId(),
                task.getPlanId(),
                task.getTaskType(),
                task.getStatus(),
                task.getInputPayload(),
                task.getOutputPayload(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
