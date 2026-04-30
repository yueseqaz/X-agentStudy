package com.xagentstudy.summary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "summary_cards")
public class SummaryCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long planId;

    private Long documentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    private String sourceDocumentName;

    @Lob
    @Column(columnDefinition = "longtext")
    private String imageData;

    @Column(nullable = false, columnDefinition = "json")
    private String contentJson;

    @Column(nullable = false, length = 64)
    private String templateType;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected SummaryCard() {
    }

    public SummaryCard(Long userId, Long planId, Long documentId, String title, String summary, String sourceDocumentName, String contentJson, String templateType) {
        this.userId = userId;
        this.planId = planId;
        this.documentId = documentId;
        this.title = title;
        this.summary = summary;
        this.sourceDocumentName = sourceDocumentName;
        this.contentJson = contentJson;
        this.templateType = templateType;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void replaceContent(String title, String summary, String contentJson) {
        this.title = title;
        this.summary = summary;
        this.contentJson = contentJson;
        this.imageData = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateImage(String imageData) {
        this.imageData = imageData;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPlanId() {
        return planId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceDocumentName() {
        return sourceDocumentName;
    }

    public String getImageData() {
        return imageData;
    }

    public String getContentJson() {
        return contentJson;
    }

    public String getTemplateType() {
        return templateType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
