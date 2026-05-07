package com.xagentstudy.qa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.GeneratedAnswer;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.profile.LearningProfileRepository;
import com.xagentstudy.rag.chunk.KnowledgeChunkResponse;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class QAService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningProfileRepository profileRepository;
    private final KnowledgeChunkService chunkService;
    private final KnowledgeDocumentRepository documentRepository;
    private final AgentGenerationService agentGenerationService;
    private final QARecordRepository qaRecordRepository;
    private final ObjectMapper objectMapper;

    public QAService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            LearningProfileRepository profileRepository,
            KnowledgeChunkService chunkService,
            KnowledgeDocumentRepository documentRepository,
            AgentGenerationService agentGenerationService,
            QARecordRepository qaRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.profileRepository = profileRepository;
        this.chunkService = chunkService;
        this.documentRepository = documentRepository;
        this.agentGenerationService = agentGenerationService;
        this.qaRecordRepository = qaRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QAResponse ask(Long planId, AskQuestionRequest request) {
        LearningPlan plan = ensurePlan(planId);
        String level = plan.getProfileId() == null
                ? "FOUNDATION"
                : profileRepository.findById(plan.getProfileId())
                .map(profile -> profile.getCurrentLevel())
                .orElse("FOUNDATION");
        List<KnowledgeChunkResponse> retrieved = chunkService.retrieve(planId, request.question(), 4);
        String planContext = buildPlanContext(plan);
        String context = buildContext(retrieved);
        GeneratedAnswer generated = agentGenerationService.generateAnswer(request.question(), level, planContext, context);
        String citations = citationsJson(retrieved);

        QARecord saved = qaRecordRepository.save(new QARecord(
                planId,
                AuthContext.currentUserId(),
                request.question(),
                generated.answer(),
                citations,
                generated.relatedPoints()
        ));
        return QAResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<QAResponse> history(Long planId) {
        ensurePlan(planId);
        return qaRecordRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                .stream()
                .map(QAResponse::from)
                .toList();
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private String buildContext(List<KnowledgeChunkResponse> chunks) {
        Map<Long, String> documentNames = documentRepository.findAllById(
                        chunks.stream().map(KnowledgeChunkResponse::documentId).distinct().toList()
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(KnowledgeDocument::getId, KnowledgeDocument::getName));
        StringBuilder builder = new StringBuilder();
        for (KnowledgeChunkResponse chunk : chunks) {
            builder.append(documentNames.getOrDefault(chunk.documentId(), "Document #" + chunk.documentId()))
                    .append(" / ")
                    .append(chunk.sourceLocation())
                    .append(":\n")
                    .append(chunk.content())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String buildPlanContext(LearningPlan plan) {
        String directionName = directionRepository.findById(plan.getDirectionId())
                .map(direction -> direction.getName() + (direction.getCategory() == null || direction.getCategory().isBlank() ? "" : " / " + direction.getCategory()))
                .orElse("未命名方向");
        StringBuilder builder = new StringBuilder();
        builder.append("学习方向：").append(directionName).append("\n");
        builder.append("计划标题：").append(plan.getTitle()).append("\n");
        if (plan.getGoal() != null && !plan.getGoal().isBlank()) {
            builder.append("计划目标：").append(plan.getGoal()).append("\n");
        }
        builder.append("计划章节与知识点：\n");
        builder.append(compactStages(plan.getStages()));
        return builder.toString().trim();
    }

    private String compactStages(String stagesJson) {
        if (stagesJson == null || stagesJson.isBlank()) {
            return "暂无章节结构";
        }
        try {
            JsonNode stages = objectMapper.readTree(stagesJson);
            if (!stages.isArray()) {
                return stagesJson.length() > 1800 ? stagesJson.substring(0, 1800) + "..." : stagesJson;
            }
            StringBuilder builder = new StringBuilder();
            int stageCount = 0;
            for (JsonNode stage : stages) {
                if (stageCount >= 8) {
                    builder.append("- 其余章节略\n");
                    break;
                }
                builder.append("- ").append(text(stage, "name", "未命名章节"));
                String focus = text(stage, "focus", "");
                if (!focus.isBlank()) {
                    builder.append("：").append(focus);
                }
                builder.append("\n");
                JsonNode points = stage.path("knowledgePoints");
                if (!points.isArray() || points.isEmpty()) {
                    points = stage.path("units").isArray() && !stage.path("units").isEmpty()
                            ? stage.path("units").get(0).path("knowledgePoints")
                            : points;
                }
                if (points.isArray()) {
                    int pointCount = 0;
                    for (JsonNode point : points) {
                        if (pointCount >= 8) {
                            builder.append("  - 其余知识点略\n");
                            break;
                        }
                        builder.append("  - ").append(text(point, "title", text(point, "pointTitle", "未命名知识点"))).append("\n");
                        pointCount++;
                    }
                }
                stageCount++;
            }
            return builder.toString().trim();
        } catch (Exception ignored) {
            return stagesJson.length() > 1800 ? stagesJson.substring(0, 1800) + "..." : stagesJson;
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private String citationsJson(List<KnowledgeChunkResponse> chunks) {
        try {
            Map<Long, String> documentNames = documentRepository.findAllById(
                            chunks.stream().map(KnowledgeChunkResponse::documentId).distinct().toList()
                    )
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(KnowledgeDocument::getId, KnowledgeDocument::getName));
            return objectMapper.writeValueAsString(chunks.stream()
                    .map(chunk -> Map.of(
                            "documentId", chunk.documentId(),
                            "documentName", documentNames.getOrDefault(chunk.documentId(), "Document #" + chunk.documentId()),
                            "chunkId", chunk.id(),
                            "sourceLocation", chunk.sourceLocation(),
                            "excerpt", excerpt(chunk.content())
                    ))
                    .toList());
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private String excerpt(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace("\n", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }
}
