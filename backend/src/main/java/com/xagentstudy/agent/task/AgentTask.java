package com.xagentstudy.agent.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agent_tasks")
public class AgentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planId;

    @Column(nullable = false, length = 64)
    private String taskType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "json")
    private String inputPayload;

    @Column(columnDefinition = "json")
    private String outputPayload;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected AgentTask() {
    }

    public AgentTask(Long planId, String taskType, String inputPayload) {
        this.planId = planId;
        this.taskType = taskType;
        this.inputPayload = inputPayload;
        this.status = "PENDING";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void markRunning() {
        this.status = "RUNNING";
        this.updatedAt = OffsetDateTime.now();
    }

    public void markSuccess(String outputPayload) {
        this.status = "SUCCESS";
        this.outputPayload = outputPayload;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getStatus() {
        return status;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
