package com.xagentstudy.rag.chunk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class KnowledgeChunkService {
    private static final int CHUNK_SIZE = 500;

    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final EmbeddingProvider embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeChunkService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            KnowledgeChunkRepository chunkRepository,
            EmbeddingProvider embeddingService,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkResponse> listByPlan(Long planId) {
        ensurePlan(planId);
        return chunkRepository.findByPlanIdOrderByDocumentIdAscChunkIndexAsc(planId)
                .stream()
                .map(KnowledgeChunkResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkResponse> listByDocument(Long planId, Long documentId) {
        ensurePlan(planId);
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .filter(chunk -> chunk.getPlanId().equals(planId))
                .map(KnowledgeChunkResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkResponse> retrieve(Long planId, String query, int limit) {
        ensurePlan(planId);
        List<String> terms = tokenize(query);
        List<Double> queryEmbedding = embeddingService.embed(query);
        return chunkRepository.findByPlanIdOrderByDocumentIdAscChunkIndexAsc(planId)
                .stream()
                .map(chunk -> {
                    int keywordScore = score(chunk.getContent(), terms);
                    double vectorScore = embeddingService.cosine(queryEmbedding, embeddingFor(chunk));
                    return new HybridScoredChunk(chunk, keywordScore, vectorScore, keywordScore + vectorScore * 12.0d);
                })
                .filter(item -> item.keywordScore() > 0 || item.vectorScore() > 0.18d)
                .sorted(Comparator.comparingDouble(HybridScoredChunk::score).reversed())
                .limit(Math.max(1, limit))
                .map(item -> KnowledgeChunkResponse.from(item.chunk()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResponse> vectorSearch(Long planId, String query, int limit) {
        ensurePlan(planId);
        List<Double> queryEmbedding = embeddingService.embed(query);
        return chunkRepository.findByPlanIdOrderByDocumentIdAscChunkIndexAsc(planId)
                .stream()
                .map(chunk -> new VectorScoredChunk(chunk, embeddingService.cosine(queryEmbedding, embeddingFor(chunk))))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingDouble(VectorScoredChunk::score).reversed())
                .limit(Math.max(1, limit))
                .map(item -> new VectorSearchResponse(KnowledgeChunkResponse.from(item.chunk()), item.score()))
                .toList();
    }

    @Transactional
    public int replaceChunks(Long planId, Long documentId, String content) {
        chunkRepository.deleteByDocumentId(documentId);
        List<KnowledgeChunk> chunks = split(content).stream()
                .map(item -> new KnowledgeChunk(
                        planId,
                        documentId,
                        item.index(),
                        item.content(),
                        "chunk-" + item.index(),
                        toEmbeddingJson(item.content())
                ))
                .toList();
        chunkRepository.saveAll(chunks);
        return chunks.size();
    }

    @Transactional
    public void deleteByDocument(Long documentId) {
        chunkRepository.deleteByDocumentId(documentId);
    }

    private List<ChunkPiece> split(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<ChunkPiece> pieces = new ArrayList<>();
        int index = 0;
        for (int start = 0; start < normalized.length(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                pieces.add(new ChunkPiece(index++, chunk));
            }
        }
        return pieces;
    }

    private List<String> tokenize(String query) {
        String normalized = query == null ? "" : query.toLowerCase();
        List<String> terms = new ArrayList<>();
        for (String part : normalized.split("[\\s,，。！？?;；:：、]+")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                terms.add(trimmed);
            }
        }
        for (int i = 0; i < normalized.length(); i += 2) {
            int end = Math.min(i + 2, normalized.length());
            String gram = normalized.substring(i, end).trim();
            if (gram.length() >= 2) {
                terms.add(gram);
            }
        }
        return terms;
    }

    private int score(String content, List<String> terms) {
        String text = content == null ? "" : content.toLowerCase();
        int score = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                score += Math.max(1, term.length());
            }
        }
        return score;
    }

    private String toEmbeddingJson(String content) {
        try {
            return objectMapper.writeValueAsString(embeddingService.embed(content));
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<Double> embeddingFor(KnowledgeChunk chunk) {
        try {
            if (chunk.getEmbedding() != null && !chunk.getEmbedding().isBlank()) {
                return objectMapper.readValue(chunk.getEmbedding(), new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
            // Fall back to generating the vector from chunk content for older rows.
        }
        return embeddingService.embed(chunk.getContent());
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private record ChunkPiece(int index, String content) {
    }

    private record HybridScoredChunk(KnowledgeChunk chunk, int keywordScore, double vectorScore, double score) {
    }

    private record VectorScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
