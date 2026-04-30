package com.xagentstudy.rag.chunk;

import java.time.OffsetDateTime;

public record KnowledgeChunkResponse(
        Long id,
        Long planId,
        Long documentId,
        Integer chunkIndex,
        String content,
        String sourceLocation,
        String vectorId,
        OffsetDateTime createdAt
) {
    public static KnowledgeChunkResponse from(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getPlanId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getSourceLocation(),
                chunk.getVectorId(),
                chunk.getCreatedAt()
        );
    }
}
