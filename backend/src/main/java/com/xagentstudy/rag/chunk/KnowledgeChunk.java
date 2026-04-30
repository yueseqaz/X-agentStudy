package com.xagentstudy.rag.chunk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private String sourceLocation;
    private String vectorId;
    @Column(columnDefinition = "json")
    private String embedding;
    private OffsetDateTime createdAt;

    protected KnowledgeChunk() {
    }

    public KnowledgeChunk(Long planId, Long documentId, Integer chunkIndex, String content, String sourceLocation, String embedding) {
        this.planId = planId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.sourceLocation = sourceLocation;
        this.vectorId = "local-hash-v1:" + planId + ":" + documentId + ":" + chunkIndex;
        this.embedding = embedding;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public String getVectorId() {
        return vectorId;
    }

    public String getEmbedding() {
        return embedding;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
