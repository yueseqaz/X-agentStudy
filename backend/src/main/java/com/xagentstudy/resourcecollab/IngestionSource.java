package com.xagentstudy.resourcecollab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_sources")
public class IngestionSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false, length = 512)
    private String baseUrl;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 64)
    private String sourceCategory;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected IngestionSource() {
    }

    public IngestionSource(Long ownerUserId, String name, String sourceType, String baseUrl, Boolean enabled, String sourceCategory) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.sourceType = sourceType;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.sourceCategory = sourceCategory;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getSourceCategory() {
        return sourceCategory;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
