package com.xagentstudy.resourcecollab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "candidate_resources")
public class CandidateResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false, length = 512)
    private String resourceUrl;

    @Column(length = 255)
    private String authorName;

    @Column(length = 512)
    private String coverImageUrl;

    private Integer durationSeconds;

    @Column(length = 512)
    private String tags;

    @Column(nullable = false, length = 32)
    private String reviewStatus;

    @Column(nullable = false, length = 32)
    private String contentCaptureMode;

    @Column(columnDefinition = "longtext")
    private String rawContent;

    @Column(columnDefinition = "text")
    private String reviewNote;

    private Long reviewedBy;
    private OffsetDateTime reviewedAt;
    private Long publishedResourceId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected CandidateResource() {
    }

    public CandidateResource(
            Long ownerUserId,
            Long sourceId,
            Long taskId,
            String sourceType,
            String title,
            String summary,
            String resourceUrl,
            String tags,
            String reviewStatus,
            String contentCaptureMode,
            String rawContent
    ) {
        this.ownerUserId = ownerUserId;
        this.sourceId = sourceId;
        this.taskId = taskId;
        this.sourceType = sourceType;
        this.title = title;
        this.summary = summary;
        this.resourceUrl = resourceUrl;
        this.tags = tags;
        this.reviewStatus = reviewStatus;
        this.contentCaptureMode = contentCaptureMode;
        this.rawContent = rawContent;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            String title,
            String summary,
            String tags,
            String authorName,
            String coverImageUrl,
            Integer durationSeconds,
            String rawContent
    ) {
        this.title = title;
        this.summary = summary;
        this.tags = tags;
        this.authorName = authorName;
        this.coverImageUrl = coverImageUrl;
        this.durationSeconds = durationSeconds;
        if (!isVideo()) {
            this.rawContent = rawContent;
        } else {
            this.rawContent = null;
            this.contentCaptureMode = "INDEX_ONLY";
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void submitForReview() {
        this.reviewStatus = "PENDING_REVIEW";
        this.reviewNote = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void review(String reviewStatus, String reviewNote, Long reviewedBy) {
        this.reviewStatus = reviewStatus;
        this.reviewNote = reviewNote;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = OffsetDateTime.now();
        this.updatedAt = this.reviewedAt;
    }

    public void markPublished(Long publishedResourceId) {
        this.publishedResourceId = publishedResourceId;
    }

    public boolean isVideo() {
        return "VIDEO".equalsIgnoreCase(sourceType);
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

    public Long getTaskId() {
        return taskId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getTags() {
        return tags;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getContentCaptureMode() {
        return contentCaptureMode;
    }

    public String getRawContent() {
        return rawContent;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public Long getPublishedResourceId() {
        return publishedResourceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
