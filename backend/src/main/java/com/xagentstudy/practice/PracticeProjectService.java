package com.xagentstudy.practice;

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
public class PracticeProjectService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningReportService reportService;
    private final ObjectMapper objectMapper;

    public PracticeProjectService(
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
    public PracticeProjectResponse getProject(Long planId) {
        LearningPlan plan = ensurePlan(planId);
        LearningReportResponse report = reportService.getReport(planId);
        StageDraft stage = currentStage(plan);
        List<String> points = stage.knowledgePoints().isEmpty() ? List.of(stage.name()) : stage.knowledgePoints();
        List<String> weakPoints = report.weakPoints();
        return new PracticeProjectResponse(
                plan.getId(),
                stage.index(),
                stage.name(),
                stage.name() + " 实战项目",
                "围绕「" + stage.focus() + "」完成一个可说明、可复盘的小项目，重点证明你能把知识点用于真实场景。",
                List.of("项目说明", "关键设计理由", "任务完成记录", "风险与改进复盘"),
                points,
                weakPoints,
                tasks(stage, points, weakPoints)
        );
    }

    @Transactional(readOnly = true)
    public PracticeProjectResultResponse submit(Long planId, SubmitPracticeProjectRequest request) {
        PracticeProjectResponse project = getProject(planId);
        Map<String, String> notes = request == null || request.taskNotes() == null ? Map.of() : request.taskNotes();
        String fullText = (request == null ? "" : nullToBlank(request.summary()) + "\n" + nullToBlank(request.reflection()) + "\n" + String.join("\n", notes.values())).trim();
        List<PracticeProjectTaskResultResponse> taskResults = project.tasks().stream()
                .map(task -> gradeTask(task, notes.get(task.id()), fullText))
                .toList();
        int taskAverage = taskResults.isEmpty() ? 0 : (int) Math.round(taskResults.stream().mapToInt(PracticeProjectTaskResultResponse::score).average().orElse(0));
        int summaryScore = Math.min(100, fullText.length() * 2);
        int totalScore = Math.min(100, Math.round(taskAverage * 0.7f + summaryScore * 0.3f));
        boolean passed = totalScore >= 70;
        List<String> covered = coveredPoints(project.knowledgePoints(), fullText);
        List<String> unstable = project.knowledgePoints().stream().filter(point -> !covered.contains(point)).limit(4).toList();
        return new PracticeProjectResultResponse(
                project.planId(),
                project.stageIndex(),
                project.stageName(),
                totalScore,
                passed,
                totalScore >= 85 ? "作品完成度高" : totalScore >= 70 ? "项目达标" : totalScore >= 50 ? "需要补强" : "未完成",
                project.stageName() + " 实战成果卡",
                passed ? "项目说明覆盖了核心任务和关键知识点，可以作为本阶段实战成果。" : "项目说明仍不完整，建议补齐任务记录、设计理由和薄弱点复盘。",
                covered,
                unstable,
                taskResults,
                passed
                        ? List.of("保存项目成果卡", "把成果写入周复盘", "进入下一阶段或追加更高难度项目")
                        : List.of("补充未完成任务说明", "回到薄弱点中心修复", "重新提交项目复盘")
        );
    }

    private PracticeProjectTaskResultResponse gradeTask(PracticeProjectTaskResponse task, String note, String fullText) {
        String value = nullToBlank(note);
        int score = Math.min(100, value.length() * 4);
        String lower = fullText.toLowerCase();
        for (String point : task.knowledgePoints()) {
            if (matchesPoint(point, lower)) {
                score += 15;
            }
        }
        if (value.contains("因为") || value.contains("所以") || lower.contains("because")) {
            score += 10;
        }
        score = Math.max(0, Math.min(100, score));
        return new PracticeProjectTaskResultResponse(
                task.id(),
                task.title(),
                score,
                score >= 70 ? "任务说明较完整，能看出实现思路和知识点应用。" : "任务说明偏少，建议补充做法、原因和验证结果。"
        );
    }

    private List<String> coveredPoints(List<String> points, String text) {
        String lower = text.toLowerCase();
        return points.stream()
                .filter(point -> matchesPoint(point, lower))
                .toList();
    }

    private boolean matchesPoint(String point, String lowerText) {
        if (point == null || point.isBlank()) {
            return false;
        }
        String lowerPoint = point.toLowerCase();
        if (lowerText.contains(lowerPoint)) {
            return true;
        }
        for (String part : lowerPoint.split("[、/，,与和\\s]+")) {
            if (part.length() >= 2 && lowerText.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private List<PracticeProjectTaskResponse> tasks(StageDraft stage, List<String> points, List<String> weakPoints) {
        List<String> safePoints = points.isEmpty() ? List.of(stage.name()) : points;
        String pointOne = safePoints.get(0);
        String pointTwo = safePoints.size() > 1 ? safePoints.get(1) : pointOne;
        String pointThree = safePoints.size() > 2 ? safePoints.get(2) : pointTwo;
        String weakText = weakPoints.isEmpty() ? pointTwo : weakPoints.get(0);
        List<PracticeProjectTaskResponse> tasks = new ArrayList<>();
        tasks.add(new PracticeProjectTaskResponse(
                "task-1",
                stage.name() + "项目：设计「" + pointOne + "」数据结构",
                "围绕阶段目标「" + stage.focus() + "」，说明项目要解决的问题，并设计和「" + pointOne + "」相关的核心数据、状态或流程。",
                safePoints.subList(0, Math.min(2, safePoints.size())),
                "能说清项目目标、核心对象、" + pointOne + " 的使用方式和关键设计理由。",
                30
        ));
        tasks.add(new PracticeProjectTaskResponse(
                "task-2",
                "实现「" + pointTwo + "」与「" + pointThree + "」核心流程",
                "写出一个能体现「" + pointTwo + "」和「" + pointThree + "」的核心功能方案、伪代码或操作步骤，并解释为什么这样设计。",
                safePoints.subList(0, Math.min(3, safePoints.size())),
                "方案能覆盖主要流程，并能解释 " + pointTwo + " 的关键取舍。",
                45
        ));
        tasks.add(new PracticeProjectTaskResponse(
                "task-3",
                "补强薄弱点「" + weakText + "」",
                "针对当前薄弱点「" + weakText + "」写出补救设计、边界情况或验证方法，并说明它会如何影响项目结果。",
                weakPoints.isEmpty() ? safePoints.subList(0, Math.min(2, safePoints.size())) : weakPoints.subList(0, Math.min(2, weakPoints.size())),
                "至少回应「" + weakText + "」，并说明如何验证。",
                30
        ));
        tasks.add(new PracticeProjectTaskResponse(
                "task-4",
                stage.name() + "成果复盘",
                "整理项目成果，明确写出「" + String.join("、", safePoints.subList(0, Math.min(4, safePoints.size()))) + "」分别如何被用到，以及还不稳定的地方。",
                safePoints.subList(0, Math.min(4, safePoints.size())),
                "能形成可展示的成果说明和真实复盘。",
                20
        ));
        return tasks;
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

    private String nullToBlank(String value) {
        return value == null ? "" : value.trim();
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
