package com.xagentstudy.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_plans")
public class LearningPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long directionId;

    private Long profileId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "text")
    private String goal;

    @Column(columnDefinition = "json")
    private String stages;

    private Integer currentStageIndex;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected LearningPlan() {
    }

    public LearningPlan(Long directionId, Long profileId, String title, String status, String goal, String stages) {
        this.directionId = directionId;
        this.profileId = profileId;
        this.title = title;
        this.status = status;
        this.goal = goal;
        this.stages = stages;
        this.currentStageIndex = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDirectionId() {
        return directionId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getGoal() {
        return goal;
    }

    public String getStages() {
        return stages;
    }

    public Integer getCurrentStageIndex() {
        return currentStageIndex;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateCurrentStageIndex(Integer currentStageIndex) {
        this.currentStageIndex = currentStageIndex;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateStages(String stages) {
        this.stages = stages;
        this.updatedAt = OffsetDateTime.now();
    }

    public void replaceGenerated(Long profileId, String title, String goal, String stages) {
        this.profileId = profileId;
        this.title = title;
        this.goal = goal;
        this.stages = stages;
        this.status = "ACTIVE";
        this.currentStageIndex = 0;
        this.updatedAt = OffsetDateTime.now();
    }
}
