package com.xagentstudy.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.resource.LearningResourceResponse;
import com.xagentstudy.resource.LearningResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceQualityService {
    private final LearningPlanRepository planRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final LearningReportService reportService;
    private final LearningResourceService resourceService;
    private final ObjectMapper objectMapper;

    public ResourceQualityService(
            LearningPlanRepository planRepository,
            KnowledgeDocumentRepository documentRepository,
            LearningReportService reportService,
            LearningResourceService resourceService,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.documentRepository = documentRepository;
        this.reportService = reportService;
        this.resourceService = resourceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResourceQualityResponse getQuality(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        List<KnowledgeDocument> documents = documentRepository.findByPlanIdOrderByUploadedAtDesc(planId);
        LearningReportResponse report = reportService.getReport(planId);
        List<LearningResourceResponse> resources = resourceService.recommended(planId);
        int pointCount = knowledgePointCount(plan.getStages());
        long parsedDocuments = documents.stream().filter(document -> "PARSE_SUCCESS".equals(document.getParseStatus()) || "PARSED".equals(document.getParseStatus())).count();
        long externalCourses = resources.stream().filter(resource -> "EXTERNAL_COURSE".equals(resource.resourceType())).count();

        List<ResourceQualityItemResponse> items = List.of(
                item(
                        "知识覆盖",
                        pointCount == 0 ? (documents.isEmpty() ? 0 : 65) : Math.min(100, (int) Math.round(parsedDocuments * 100.0 / pointCount)),
                        "已解析资料 " + parsedDocuments + " 份，计划知识点 " + pointCount + " 个。",
                        List.of(documents.isEmpty() ? "暂无讲义资料" : "最近资料：" + documents.get(0).getName())
                ),
                item(
                        "多模态完整度",
                        multimodalScore(documents, report, resources, pointCount),
                        "综合讲义、导图、题目、课程资源和学习包入口。",
                        List.of("讲义 " + documents.size() + " 份", "题目 " + report.questionCount() + " 道", "课程资源 " + resources.size() + " 个")
                ),
                item(
                        "测验闭环",
                        report.questionCount() == 0 ? 0 : Math.min(100, Math.max(45, (int) Math.round(report.answeredQuestionCount() * 100.0 / report.questionCount()))),
                        "通过作答、错题和复习数据检验生成资源是否有效。",
                        List.of("已作答 " + report.answeredQuestionCount() + " 道", "正确率 " + report.accuracyRate() + "%", "薄弱点 " + report.weakPoints().size() + " 个")
                ),
                item(
                        "个性化匹配",
                        personalizationScore(report, resources, externalCourses),
                        "根据计划主题、薄弱点和课程资源匹配度评估。",
                        List.of("掌握度 " + report.masteryScore(), "Bilibili 课程 " + externalCourses + " 个")
                )
        );
        int overall = Math.round((float) items.stream().mapToInt(ResourceQualityItemResponse::score).average().orElse(0));
        return new ResourceQualityResponse(planId, overall, label(overall), items, suggestions(items, report, documents, resources));
    }

    private ResourceQualityItemResponse item(String name, int score, String reason, List<String> evidence) {
        String status = score >= 80 ? "优秀" : score >= 60 ? "可用" : "待补强";
        return new ResourceQualityItemResponse(name, Math.max(0, Math.min(100, score)), status, reason, evidence);
    }

    private int multimodalScore(List<KnowledgeDocument> documents, LearningReportResponse report, List<LearningResourceResponse> resources, int pointCount) {
        int score = 0;
        if (!documents.isEmpty()) {
            score += 25;
        }
        if (pointCount > 0) {
            score += 20;
        }
        if (report.questionCount() > 0) {
            score += 25;
        }
        if (!resources.isEmpty()) {
            score += 20;
        }
        if (report.answeredQuestionCount() > 0) {
            score += 10;
        }
        return score;
    }

    private int personalizationScore(LearningReportResponse report, List<LearningResourceResponse> resources, long externalCourses) {
        int score = 35;
        if (!report.weakPoints().isEmpty()) {
            score += 20;
        }
        if (!resources.isEmpty()) {
            score += 20;
        }
        if (externalCourses > 0) {
            score += 10;
        }
        score += Math.min(15, report.masteryScore() / 6);
        return Math.min(100, score);
    }

    private List<String> suggestions(
            List<ResourceQualityItemResponse> items,
            LearningReportResponse report,
            List<KnowledgeDocument> documents,
            List<LearningResourceResponse> resources
    ) {
        List<String> suggestions = new ArrayList<>();
        for (ResourceQualityItemResponse item : items) {
            if (item.score() < 60) {
                suggestions.add("优先补强「" + item.name() + "」。");
            }
        }
        if (documents.isEmpty()) {
            suggestions.add("先为核心知识点生成讲义，后续视频和测验质量会更稳定。");
        }
        if (report.questionCount() == 0) {
            suggestions.add("生成至少 3 道测验题，让系统能判断资源是否真正被掌握。");
        }
        if (resources.isEmpty()) {
            suggestions.add("补充匹配课程资源，形成内部讲义与外部课程互补。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前资源闭环完整，可以追加更高难度实操项目。");
        }
        return suggestions.stream().distinct().limit(4).toList();
    }

    private String label(int score) {
        if (score >= 85) {
            return "资源闭环优秀";
        }
        if (score >= 65) {
            return "资源可用于学习";
        }
        return "资源仍需补强";
    }

    private int knowledgePointCount(String stagesJson) {
        if (stagesJson == null || stagesJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode stages = objectMapper.readTree(stagesJson);
            int count = 0;
            if (stages.isArray()) {
                for (JsonNode stage : stages) {
                    JsonNode units = stage.path("units");
                    if (!units.isArray()) {
                        continue;
                    }
                    for (JsonNode unit : units) {
                        JsonNode points = unit.path("knowledgePoints");
                        if (points.isArray()) {
                            count += points.size();
                        }
                    }
                }
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
