package com.xagentstudy.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_resources")
public class LearningResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long uploaderUserId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 32)
    private String resourceType;

    @Column(nullable = false)
    private String originalFilename;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 120)
    private String subjectName;

    @Column(nullable = false, length = 160)
    private String subjectScope;

    @Column(nullable = false, length = 512)
    private String tags;

    @Column(nullable = false)
    private Long fileSize;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected LearningResource() {
    }

    public LearningResource(
            Long uploaderUserId,
            String title,
            String description,
            String resourceType,
            String originalFilename,
            String contentType,
            String storageKey,
            String subjectName,
            String subjectScope,
            String tags,
            Long fileSize
    ) {
        this.uploaderUserId = uploaderUserId;
        this.title = title;
        this.description = description;
        this.resourceType = resourceType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.subjectName = subjectName;
        this.subjectScope = subjectScope;
        this.tags = tags;
        this.fileSize = fileSize == null ? 0L : fileSize;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    LearningResource(
            Long uploaderUserId,
            String title,
            String description,
            String resourceType,
            String originalFilename,
            String contentType,
            String storageKey,
            String subjectName,
            String subjectScope,
            String tags,
            Long fileSize,
            OffsetDateTime createdAt
    ) {
        this(uploaderUserId, title, description, resourceType, originalFilename, contentType, storageKey, subjectName, subjectScope, tags, fileSize);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUploaderUserId() {
        return uploaderUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectScope() {
        return subjectScope;
    }

    public String getTags() {
        return tags;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
