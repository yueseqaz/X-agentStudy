package com.xagentstudy.resourcecollab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "collaborator_applications")
public class CollaboratorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(length = 512)
    private String expertise;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "text")
    private String reviewNote;

    private Long reviewedBy;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected CollaboratorApplication() {
    }

    public CollaboratorApplication(Long userId, String reason, String expertise) {
        this.userId = userId;
        this.reason = reason;
        this.expertise = expertise;
        this.status = "PENDING";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void resubmit(String reason, String expertise) {
        this.reason = reason;
        this.expertise = expertise;
        this.status = "PENDING";
        this.reviewNote = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void review(String status, String reviewNote, Long reviewedBy) {
        this.status = status;
        this.reviewNote = reviewNote;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = OffsetDateTime.now();
        this.updatedAt = this.reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public String getExpertise() {
        return expertise;
    }

    public String getStatus() {
        return status;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
