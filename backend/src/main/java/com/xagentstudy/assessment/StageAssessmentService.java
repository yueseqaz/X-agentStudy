package com.xagentstudy.assessment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.report.LearningReportResponse;
import com.xagentstudy.report.LearningReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StageAssessmentService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningReportService reportService;
    private final ObjectMapper objectMapper;

    public StageAssessmentService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            LearningReportService reportService,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StageAssessmentResponse getAssessment(Long planId) {
        LearningPlan plan = ensurePlan(planId);
        LearningReportResponse report = reportService.getReport(planId);
        StageDraft stage = currentStage(plan);
        List<String> points = stage.knowledgePoints().isEmpty() ? List.of(stage.name()) : stage.knowledgePoints();
        int readinessScore = Math.max(0, Math.min(100, report.masteryScore()));
        return new StageAssessmentResponse(
                plan.getId(),
                stage.index(),
                stage.name(),
                stage.focus(),
                readinessScore,
                readinessLabel(readinessScore),
                points,
                report.weakPoints(),
                challenges(stage, points, report.weakPoints())
        );
    }

    @Transactional(readOnly = true)
    public StageAssessmentResultResponse submit(Long planId, SubmitStageAssessmentRequest request) {
        StageAssessmentResponse assessment = getAssessment(planId);
        Map<String, String> answers = request == null || request.answers() == null ? Map.of() : request.answers();
        List<StageAssessmentItemResultResponse> results = assessment.challenges().stream()
                .map(challenge -> grade(challenge, assessment.knowledgePoints(), answers.get(challenge.id())))
                .toList();
        int totalScore = results.isEmpty() ? 0 : (int) Math.round(results.stream().mapToInt(StageAssessmentItemResultResponse::score).average().orElse(0));
        boolean passed = totalScore >= 70;
        String level = totalScore >= 85 ? "掌握稳定" : totalScore >= 70 ? "可以进入下一阶段" : totalScore >= 50 ? "基本掌握" : "未达标";
        return new StageAssessmentResultResponse(
                assessment.planId(),
                assessment.stageIndex(),
                assessment.stageName(),
                totalScore,
                passed,
                level,
                passed ? "本阶段核心能力已通过验收，可以进入下一阶段学习。" : "本阶段仍需补强，建议先完成薄弱点修复后再次验收。",
                assessment.stageName() + " 阶段通过卡片",
                results,
                passed
                        ? List.of("进入下一阶段", "生成周复盘记录成果", "保留通过卡片用于学习成果展示")
                        : List.of("回到薄弱点中心修复错题", "完成复习清单", "重新提交阶段验收")
        );
    }

    private StageAssessmentItemResultResponse grade(StageAssessmentChallengeResponse challenge, List<String> points, String answer) {
        String value = answer == null ? "" : answer.trim();
        int score = Math.min(100, value.length() * 2);
        String lower = value.toLowerCase();
        for (String point : points) {
            if (!point.isBlank() && lower.contains(point.toLowerCase())) {
                score += 15;
            }
        }
        if (value.contains("因为") || value.contains("所以") || lower.contains("because")) {
            score += 10;
        }
        if (value.contains("例") || lower.contains("case") || lower.contains("example")) {
            score += 10;
        }
        score = Math.max(0, Math.min(100, score));
        String feedback = score >= 70
                ? "回答覆盖了核心概念，并能结合场景说明。"
                : "回答还不够完整，建议补充核心概念、原因和一个具体例子。";
        return new StageAssessmentItemResultResponse(challenge.id(), challenge.title(), score, feedback);
    }

    private List<StageAssessmentChallengeResponse> challenges(StageDraft stage, List<String> points, List<String> weakPoints) {
        String pointText = String.join("、", points.subList(0, Math.min(4, points.size())));
        String primaryPoint = points.isEmpty() ? stage.name() : points.get(0);
        String secondPoint = points.size() > 1 ? points.get(1) : primaryPoint;
        String weakText = weakPoints.isEmpty() ? "当前暂无明显薄弱点" : String.join("、", weakPoints.subList(0, Math.min(3, weakPoints.size())));
        return List.of(
                new StageAssessmentChallengeResponse(
                        "concept-check",
                        stage.name() + "：概念关系验收",
                        "围绕「" + stage.name() + "」，解释「" + primaryPoint + "」和「" + secondPoint + "」分别解决什么问题、有什么关系，并说明它们如何支撑阶段目标「" + stage.focus() + "」。",
                        "必须覆盖：" + pointText,
                        12
                ),
                new StageAssessmentChallengeResponse(
                        "case-transfer",
                        stage.name() + "：场景迁移题",
                        "围绕「" + stage.name() + "」构造一个和「" + stage.focus() + "」相关的真实使用场景，说明你会怎样应用「" + pointText + "」完成设计、验证效果并处理边界情况。",
                        "需要结合阶段目标：" + stage.focus(),
                        18
                ),
                new StageAssessmentChallengeResponse(
                        "interview-defense",
                        stage.name() + "：薄弱点追问",
                        "如果面试官围绕「" + weakText + "」追问风险、误区和取舍，你会如何回答？请结合「" + pointText + "」给出判断依据。",
                        "优先回应薄弱点：" + weakText,
                        15
                )
        );
    }

    private String readinessLabel(int score) {
        if (score >= 80) {
            return "适合验收";
        }
        if (score >= 50) {
            return "可以尝试";
        }
        return "建议先补弱";
    }

    private StageDraft currentStage(LearningPlan plan) {
        List<Map<String, Object>> stages = parseStages(plan.getStages());
        if (stages.isEmpty()) {
            return new StageDraft(0, plan.getTitle(), plan.getGoal(), List.of());
        }
        int index = Math.max(0, Math.min(plan.getCurrentStageIndex() == null ? 0 : plan.getCurrentStageIndex(), stages.size() - 1));
        Map<String, Object> stage = stages.get(index);
        String name = text(stage.get("name"), "当前阶段");
        String focus = text(stage.get("focus"), plan.getGoal());
        Set<String> points = new LinkedHashSet<>();
        Object unitsValue = stage.get("units");
        if (unitsValue instanceof List<?> units) {
            for (Object unitValue : units) {
                if (unitValue instanceof Map<?, ?> unit) {
                    Object knowledgePoints = unit.get("knowledgePoints");
                    if (knowledgePoints instanceof List<?> items) {
                        for (Object itemValue : items) {
                            if (itemValue instanceof Map<?, ?> item) {
                                points.add(text(item.get("title"), ""));
                            }
                        }
                    }
                }
            }
        }
        Object tasksValue = stage.get("tasks");
        if (points.isEmpty() && tasksValue instanceof List<?> tasks) {
            for (Object task : tasks) {
                points.add(text(task, ""));
            }
        }
        return new StageDraft(index, name, focus, points.stream().filter(point -> !point.isBlank()).limit(8).toList());
    }

    private List<Map<String, Object>> parseStages(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback == null ? "" : fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? (fallback == null ? "" : fallback) : text;
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private record StageDraft(int index, String name, String focus, List<String> knowledgePoints) {
    }
}
