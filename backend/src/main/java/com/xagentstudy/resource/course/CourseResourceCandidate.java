package com.xagentstudy.resource.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_resource_candidates")
public class CourseResourceCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 768)
    private String courseUrl;

    @Column(nullable = false, length = 120)
    private String sourceName;

    @Column(length = 768)
    private String coverUrl;

    @Column(nullable = false, length = 120)
    private String subjectName;

    @Column(nullable = false, length = 160)
    private String subjectScope;

    @Column(nullable = false, length = 512)
    private String tags;

    @Column(length = 80)
    private String difficulty;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Long createdBy;

    private Long publishedResourceId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected CourseResourceCandidate() {
    }

    public CourseResourceCandidate(CourseCrawlerCandidate candidate, Long createdBy) {
        this.title = candidate.title();
        this.description = candidate.description();
        this.courseUrl = candidate.url();
        this.sourceName = candidate.source();
        this.coverUrl = candidate.coverUrl();
        this.subjectName = candidate.subjectName();
        this.subjectScope = candidate.subjectScope();
        this.tags = String.join(",", candidate.tags());
        this.difficulty = candidate.difficulty();
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void publish(Long resourceId) {
        this.status = "PUBLISHED";
        this.publishedResourceId = resourceId;
        this.updatedAt = OffsetDateTime.now();
    }

    public void detachPublishedResource() {
        this.status = "PENDING";
        this.publishedResourceId = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject() {
        this.status = "REJECTED";
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCourseUrl() { return courseUrl; }
    public String getSourceName() { return sourceName; }
    public String getCoverUrl() { return coverUrl; }
    public String getSubjectName() { return subjectName; }
    public String getSubjectScope() { return subjectScope; }
    public String getTags() { return tags; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }
    public Long getPublishedResourceId() { return publishedResourceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
