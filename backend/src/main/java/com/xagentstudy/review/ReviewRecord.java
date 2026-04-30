package com.xagentstudy.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_records")
public class ReviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String knowledgePoint;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false)
    private Integer priorityLevel;

    @Column(nullable = false)
    private Boolean completed;

    private OffsetDateTime recommendedAt;

    private OffsetDateTime completedAt;
    private OffsetDateTime dueAt;
    private OffsetDateTime lastReviewedAt;
    private Integer intervalDays;
    private Double easeFactor;
    private Integer masteryScore;

    protected ReviewRecord() {
    }

    public ReviewRecord(Long planId, Long userId, String knowledgePoint, String reason, Integer priorityLevel) {
        this.planId = planId;
        this.userId = userId;
        this.knowledgePoint = knowledgePoint;
        this.reason = reason;
        this.priorityLevel = priorityLevel;
        this.completed = false;
        this.recommendedAt = OffsetDateTime.now();
        this.dueAt = this.recommendedAt;
        this.intervalDays = 1;
        this.easeFactor = 2.5d;
        this.masteryScore = 0;
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

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public String getReason() {
        return reason;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public OffsetDateTime getRecommendedAt() {
        return recommendedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public Integer getMasteryScore() {
        return masteryScore;
    }

    public void complete() {
        complete(4);
    }

    public void reopen(String reason, Integer priorityLevel) {
        this.completed = false;
        this.completedAt = null;
        this.reason = reason;
        this.priorityLevel = Math.max(this.priorityLevel, priorityLevel == null ? 1 : priorityLevel);
        this.dueAt = OffsetDateTime.now();
        this.recommendedAt = OffsetDateTime.now();
    }

    public void complete(int quality) {
        this.completed = true;
        this.completedAt = OffsetDateTime.now();
        this.lastReviewedAt = this.completedAt;
        int boundedQuality = Math.max(1, Math.min(5, quality));
        this.masteryScore = Math.min(100, this.masteryScore + boundedQuality * 10);
        this.easeFactor = Math.max(1.3d, this.easeFactor + (0.1d - (5 - boundedQuality) * 0.08d));
        this.intervalDays = boundedQuality < 3 ? 1 : Math.max(1, (int) Math.round(this.intervalDays * this.easeFactor));
        this.dueAt = this.completedAt.plusDays(this.intervalDays);
    }
}
