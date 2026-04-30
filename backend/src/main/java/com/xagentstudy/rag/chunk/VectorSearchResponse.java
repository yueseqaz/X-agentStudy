package com.xagentstudy.rag.chunk;

public record VectorSearchResponse(
        KnowledgeChunkResponse chunk,
        double score
) {
}
