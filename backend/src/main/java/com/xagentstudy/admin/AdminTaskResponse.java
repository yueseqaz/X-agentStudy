package com.xagentstudy.admin;

import com.xagentstudy.agent.task.AgentTask;

import java.time.OffsetDateTime;

public record AdminTaskResponse(
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
    public static AdminTaskResponse from(AgentTask task) {
        return new AdminTaskResponse(
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
