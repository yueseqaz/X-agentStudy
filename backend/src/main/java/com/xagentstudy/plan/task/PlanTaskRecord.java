package com.xagentstudy.plan.task;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "plan_task_records")
public class PlanTaskRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long planId;
    private Long userId;
    private Integer stageIndex;
    private Integer taskIndex;
    private String taskText;
    private Boolean completed;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected PlanTaskRecord() {
    }

    public PlanTaskRecord(Long planId, Long userId, Integer stageIndex, Integer taskIndex, String taskText) {
        this.planId = planId;
        this.userId = userId;
        this.stageIndex = stageIndex;
        this.taskIndex = taskIndex;
        this.taskText = taskText;
        this.completed = false;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getStageIndex() {
        return stageIndex;
    }

    public Integer getTaskIndex() {
        return taskIndex;
    }

    public String getTaskText() {
        return taskText;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
        this.completedAt = Boolean.TRUE.equals(completed) ? OffsetDateTime.now() : null;
        this.updatedAt = OffsetDateTime.now();
    }
}
