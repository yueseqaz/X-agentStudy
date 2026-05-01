package com.xagentstudy.resourcecollab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_tasks")
public class IngestionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false, length = 512)
    private String targetUrl;

    @Column(length = 255)
    private String titleHint;

    @Column(columnDefinition = "text")
    private String summaryHint;

    @Column(length = 512)
    private String tags;

    @Column(nullable = false, length = 32)
    private String status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected IngestionTask() {
    }

    public IngestionTask(Long ownerUserId, Long sourceId, String sourceType, String targetUrl, String status) {
        this.ownerUserId = ownerUserId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.targetUrl = targetUrl;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void enrich(String titleHint, String summaryHint, String tags) {
        this.titleHint = titleHint;
        this.summaryHint = summaryHint;
        this.tags = tags;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markCompleted() {
        this.status = "COLLECTED";
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getTitleHint() {
        return titleHint;
    }

    public String getSummaryHint() {
        return summaryHint;
    }

    public String getTags() {
        return tags;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
