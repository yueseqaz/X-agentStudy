package com.xagentstudy.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "documents")
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 32)
    private String parseStatus;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "json")
    private String sectionsJson;

    @Column(columnDefinition = "json")
    private String keyPointsJson;

    @Column(columnDefinition = "json")
    private String knowledgeTreeJson;

    private Integer contentLength;

    @Column(columnDefinition = "text")
    private String parseError;

    @Column(nullable = false, length = 32)
    private String learningStatus;

    private OffsetDateTime uploadedAt;
    private OffsetDateTime updatedAt;

    protected KnowledgeDocument() {
    }

    public KnowledgeDocument(Long planId, String name, String type, String storageKey) {
        this.planId = planId;
        this.name = name;
        this.type = type;
        this.storageKey = storageKey;
        this.parseStatus = "PARSING";
        this.summary = "资料已上传，正在进入解析队列。";
        this.learningStatus = "NOT_STARTED";
        this.uploadedAt = OffsetDateTime.now();
        this.updatedAt = this.uploadedAt;
    }

    public void markParseSuccess(String summary) {
        markParseSuccess(summary, "[]", "[]", "[]", 0);
    }

    public void markParseSuccess(String summary, String sectionsJson, String keyPointsJson, String knowledgeTreeJson, Integer contentLength) {
        this.parseStatus = "PARSE_SUCCESS";
        this.summary = summary;
        this.sectionsJson = sectionsJson;
        this.keyPointsJson = keyPointsJson;
        this.knowledgeTreeJson = knowledgeTreeJson;
        this.contentLength = contentLength;
        this.parseError = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markParseFailed(String parseError) {
        this.parseStatus = "PARSE_FAILED";
        this.parseError = parseError;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateLearningStatus(String learningStatus) {
        if (learningStatus == null || learningStatus.isBlank()) {
            return;
        }
        this.learningStatus = learningStatus;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public String getSummary() {
        return summary;
    }

    public String getSectionsJson() {
        return sectionsJson;
    }

    public String getKeyPointsJson() {
        return keyPointsJson;
    }

    public String getKnowledgeTreeJson() {
        return knowledgeTreeJson;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public String getParseError() {
        return parseError;
    }

    public String getLearningStatus() {
        return learningStatus == null ? "NOT_STARTED" : learningStatus;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
