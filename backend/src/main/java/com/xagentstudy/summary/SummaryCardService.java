package com.xagentstudy.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.AgentGenerationService.SummaryCardDraft;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.rag.chunk.KnowledgeChunkResponse;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class SummaryCardService {
    private static final int MAX_CONTEXT_LENGTH = 12000;

    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkService chunkService;
    private final AgentGenerationService agentGenerationService;
    private final SummaryCardRepository cardRepository;
    private final ObjectMapper objectMapper;

    public SummaryCardService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkService chunkService,
            AgentGenerationService agentGenerationService,
            SummaryCardRepository cardRepository,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.documentRepository = documentRepository;
        this.chunkService = chunkService;
        this.agentGenerationService = agentGenerationService;
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SummaryCardResponse> list(Long planId) {
        ensurePlan(planId);
        return cardRepository.findByPlanIdAndUserIdOrderByCreatedAtDesc(planId, AuthContext.currentUserId())
                .stream()
                .map(SummaryCardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryCardResponse detail(Long planId, Long cardId) {
        ensurePlan(planId);
        return SummaryCardResponse.from(ensureCard(planId, cardId));
    }

    @Transactional
    public SummaryCardResponse generate(Long planId, Long documentId) {
        LearningPlan plan = ensurePlan(planId);
        KnowledgeDocument document = ensureDocument(planId, documentId);
        SummaryCardDraft draft = createDraft(plan, document);
        SummaryCard saved = cardRepository.save(new SummaryCard(
                AuthContext.currentUserId(),
                planId,
                documentId,
                draft.title(),
                draft.summary(),
                document.getName(),
                toJson(draft),
                "KNOWLEDGE_SUMMARY"
        ));
        return SummaryCardResponse.from(saved);
    }

    @Transactional
    public SummaryCardResponse regenerate(Long planId, Long cardId) {
        SummaryCard card = ensureCard(planId, cardId);
        if (card.getDocumentId() == null) {
            throw new BusinessException("DOCUMENT_REQUIRED", "Summary card has no source document");
        }
        LearningPlan plan = ensurePlan(planId);
        KnowledgeDocument document = ensureDocument(planId, card.getDocumentId());
        SummaryCardDraft draft = createDraft(plan, document);
        card.replaceContent(draft.title(), draft.summary(), toJson(draft));
        return SummaryCardResponse.from(card);
    }

    @Transactional
    public SummaryCardResponse saveImage(Long planId, Long cardId, SaveSummaryCardImageRequest request) {
        SummaryCard card = ensureCard(planId, cardId);
        String imageData = request == null ? "" : request.imageData();
        if (imageData == null || !imageData.startsWith("data:image/png;base64,")) {
            throw new BusinessException("INVALID_IMAGE_DATA", "PNG image data is required");
        }
        card.updateImage(imageData);
        return SummaryCardResponse.from(card);
    }

    @Transactional
    public void delete(Long planId, Long cardId) {
        SummaryCard card = ensureCard(planId, cardId);
        cardRepository.delete(card);
    }

    @Transactional
    public void deleteByPlan(Long planId) {
        cardRepository.deleteByPlanId(planId);
    }

    @Transactional
    public void deleteByDocument(Long documentId) {
        cardRepository.deleteByDocumentId(documentId);
    }

    private SummaryCardDraft createDraft(LearningPlan plan, KnowledgeDocument document) {
        String content = documentText(plan.getId(), document);
        return agentGenerationService.generateSummaryCard(plan.getId(), plan.getTitle(), document.getId(), document.getName(), content);
    }

    private String documentText(Long planId, KnowledgeDocument document) {
        List<KnowledgeChunkResponse> chunks = chunkService.listByDocument(planId, document.getId());
        String content = chunks.stream()
                .map(KnowledgeChunkResponse::content)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (left, right) -> left + "\n\n" + right)
                .trim();
        if (content.isBlank()) {
            try {
                content = Files.readString(Path.of(document.getStorageKey()), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                content = "";
            }
        }
        if (content.isBlank()) {
            content = document.getSummary() + "\n" + document.getKeyPointsJson() + "\n" + document.getSectionsJson();
        }
        return content.length() <= MAX_CONTEXT_LENGTH ? content : content.substring(0, MAX_CONTEXT_LENGTH);
    }

    private SummaryCard ensureCard(Long planId, Long cardId) {
        return cardRepository.findById(cardId)
                .filter(card -> card.getPlanId().equals(planId) && card.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Summary card not found"));
    }

    private KnowledgeDocument ensureDocument(Long planId, Long documentId) {
        return documentRepository.findById(documentId)
                .filter(document -> document.getPlanId().equals(planId))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Document not found"));
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private String toJson(SummaryCardDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (Exception ex) {
            throw new BusinessException("SUMMARY_CARD_SERIALIZE_FAILED", "Unable to serialize summary card content");
        }
    }
}
