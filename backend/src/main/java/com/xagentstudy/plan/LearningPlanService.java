package com.xagentstudy.plan;

import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.GeneratedPlan;
import com.xagentstudy.agent.task.AgentTaskRepository;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentService;
import com.xagentstudy.plan.task.PlanTaskRecord;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.plan.task.PlanTaskResponse;
import com.xagentstudy.plan.task.UpdatePlanTaskRequest;
import com.xagentstudy.profile.LearningProfile;
import com.xagentstudy.profile.LearningProfileRepository;
import com.xagentstudy.qa.QARecordRepository;
import com.xagentstudy.quiz.AnswerRecordRepository;
import com.xagentstudy.quiz.Question;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.report.LearningReportResponse;
import com.xagentstudy.report.LearningReportService;
import com.xagentstudy.review.ReviewRecordRepository;
import com.xagentstudy.summary.SummaryCardService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningPlanService {
    private static final SecureRandom SHARE_RANDOM = new SecureRandom();
    private static final char[] SHARE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private final LearningDirectionRepository directionRepository;
    private final LearningProfileRepository profileRepository;
    private final LearningPlanRepository planRepository;
    private final AgentGenerationService agentGenerationService;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final LearningReportService reportService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentService documentService;
    private final QARecordRepository qaRecordRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final SummaryCardService summaryCardService;
    private final BillingService billingService;
    private final ObjectMapper objectMapper;

    public LearningPlanService(
            LearningDirectionRepository directionRepository,
            LearningProfileRepository profileRepository,
            LearningPlanRepository planRepository,
            AgentGenerationService agentGenerationService,
            PlanTaskRecordRepository taskRecordRepository,
            LearningReportService reportService,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeDocumentService documentService,
            QARecordRepository qaRecordRepository,
            QuestionRepository questionRepository,
            AnswerRecordRepository answerRecordRepository,
            ReviewRecordRepository reviewRecordRepository,
            AgentTaskRepository agentTaskRepository,
            SummaryCardService summaryCardService,
            BillingService billingService,
            ObjectMapper objectMapper
    ) {
        this.directionRepository = directionRepository;
        this.profileRepository = profileRepository;
        this.planRepository = planRepository;
        this.agentGenerationService = agentGenerationService;
        this.taskRecordRepository = taskRecordRepository;
        this.reportService = reportService;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.qaRecordRepository = qaRecordRepository;
        this.questionRepository = questionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.summaryCardService = summaryCardService;
        this.billingService = billingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PlanSummaryResponse> listMine() {
        List<LearningDirection> directions = directionRepository
                .findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(AuthContext.currentUserId());
        if (directions.isEmpty()) {
            return List.of();
        }
        Map<Long, LearningDirection> directionMap = directions.stream()
                .collect(Collectors.toMap(LearningDirection::getId, Function.identity()));
        Map<Long, LearningPlan> latestPlanByDirection = new java.util.LinkedHashMap<>();
        for (LearningPlan plan : planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionMap.keySet().stream().toList())) {
            latestPlanByDirection.putIfAbsent(plan.getDirectionId(), plan);
        }
        return latestPlanByDirection.values().stream()
                .map(plan -> PlanSummaryResponse.from(plan, directionMap.get(plan.getDirectionId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse get(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        LearningDirection direction = ensureDirection(plan.getDirectionId());
        return PlanResponse.from(plan, direction);
    }

    @Transactional
    public PlanShareResponse enableShare(Long planId) {
        LearningPlan plan = ensurePlanEntity(planId);
        if (plan.getShareCode() == null || plan.getShareCode().isBlank()) {
            plan.enableShare(nextShareCode());
        }
        return PlanShareResponse.from(plan);
    }

    @Transactional(readOnly = true)
    public PublicPlanResponse getPublicPlan(String shareCode) {
        LearningPlan plan = planRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Shared learning plan not found"));
        LearningDirection direction = directionRepository.findById(plan.getDirectionId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
        return PublicPlanResponse.from(plan, direction);
    }

    @Transactional
    public PlanResponse applySharedPlan(String shareCode) {
        LearningPlan sourcePlan = planRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Shared learning plan not found"));
        LearningDirection sourceDirection = directionRepository.findById(sourcePlan.getDirectionId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
        billingService.ensurePlanSlotAvailable();
        LearningDirection copiedDirection = directionRepository.save(new LearningDirection(
                AuthContext.currentUserId(),
                sourceDirection.getName(),
                sourceDirection.getCategory(),
                sourceDirection.getDescription()
        ));
        LearningPlan copiedPlan = planRepository.save(new LearningPlan(
                copiedDirection.getId(),
                null,
                sourcePlan.getTitle(),
                "ACTIVE",
                sourcePlan.getGoal(),
                sourcePlan.getStages()
        ));
        syncTaskRecords(copiedPlan);
        refreshCurrentStage(copiedPlan);
        return PlanResponse.from(copiedPlan, copiedDirection);
    }

    @Transactional(readOnly = true)
    public PlanResponse latest(Long directionId) {
        LearningDirection direction = ensureDirection(directionId);
        return planRepository.findFirstByDirectionIdOrderByCreatedAtDesc(directionId)
                .map(plan -> PlanResponse.from(plan, direction))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
    }

    @Transactional
    public PlanResponse generate(Long directionId) {
        LearningDirection direction = ensureDirection(directionId);
        LearningProfile profile = profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(directionId)
                .orElseThrow(() -> new BusinessException("PROFILE_REQUIRED", "Generate a learning profile first"));

        GeneratedPlan generated = agentGenerationService.generatePlan(direction, profile);
        java.util.Optional<LearningPlan> existing = planRepository.findFirstByDirectionIdOrderByCreatedAtDesc(directionId);
        if (existing.isEmpty()) {
            billingService.ensurePlanSlotAvailable();
        }
        LearningPlan plan = existing
                .orElseGet(() -> planRepository.save(new LearningPlan(
                        directionId,
                        profile.getId(),
                        generated.title(),
                        "ACTIVE",
                        generated.goal(),
                        generated.stages()
                )));
        plan.replaceGenerated(profile.getId(), generated.title(), generated.goal(), generated.stages());
        taskRecordRepository.deleteByPlanIdAndUserId(plan.getId(), AuthContext.currentUserId());
        syncTaskRecords(plan);
        refreshCurrentStage(plan);
        return PlanResponse.from(plan, direction);
    }

    @Transactional
    public PlanResponse regenerateStructure(Long planId) {
        LearningPlan plan = ensurePlanEntity(planId);
        LearningDirection direction = ensureDirection(plan.getDirectionId());
        LearningProfile profile = plan.getProfileId() == null
                ? profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(plan.getDirectionId())
                .orElseThrow(() -> new BusinessException("PROFILE_REQUIRED", "Generate a learning profile first"))
                : profileRepository.findById(plan.getProfileId())
                .orElseThrow(() -> new BusinessException("PROFILE_REQUIRED", "Learning profile not found"));

        GeneratedPlan generated = agentGenerationService.generatePlan(direction, profile);
        plan.updateStages(generated.stages());
        taskRecordRepository.deleteByPlanIdAndUserId(plan.getId(), AuthContext.currentUserId());
        syncTaskRecords(plan);
        refreshCurrentStage(plan);
        return PlanResponse.from(plan, direction);
    }

    @Transactional
    public List<PlanTaskResponse> listTasks(Long planId) {
        LearningPlan plan = ensurePlanEntity(planId);
        syncTaskRecords(plan);
        refreshCurrentStage(plan);
        return taskRecordRepository.findByPlanIdAndUserIdOrderByStageIndexAscTaskIndexAsc(planId, AuthContext.currentUserId())
                .stream()
                .map(PlanTaskResponse::from)
                .toList();
    }

    @Transactional
    public PlanTaskResponse updateTask(Long planId, Integer stageIndex, Integer taskIndex, UpdatePlanTaskRequest request) {
        LearningPlan plan = ensurePlanEntity(planId);
        syncTaskRecords(plan);
        PlanTaskRecord record = taskRecordRepository
                .findByPlanIdAndUserIdAndStageIndexAndTaskIndex(planId, AuthContext.currentUserId(), stageIndex, taskIndex)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Plan task not found"));
        record.setCompleted(request.completed());
        refreshCurrentStage(plan);
        return PlanTaskResponse.from(record);
    }

    @Transactional(readOnly = true)
    public PlanAdjustmentResponse adjustments(Long planId) {
        ensurePlanEntity(planId);
        LearningReportResponse report = reportService.getReport(planId);
        long totalTasks = taskRecordRepository.countByPlanIdAndUserId(planId, AuthContext.currentUserId());
        long completedTasks = taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrue(planId, AuthContext.currentUserId());
        double taskCompletionRate = totalTasks == 0 ? 0 : Math.round(completedTasks * 1000.0 / totalTasks) / 10.0;

        List<String> risks = new ArrayList<>();
        if (taskCompletionRate < 50) {
            risks.add("计划任务完成率偏低，建议缩短每日任务粒度。");
        }
        if (report.accuracyRate() < 70 && report.answeredQuestionCount() > 0) {
            risks.add("测验正确率低于 70%，需要先巩固薄弱点再推进新阶段。");
        }
        if (!report.weakPoints().isEmpty()) {
            risks.add("薄弱知识点集中在：" + String.join("、", report.weakPoints().subList(0, Math.min(3, report.weakPoints().size()))) + "。");
        }

        List<String> adjustments = new ArrayList<>();
        adjustments.add(taskCompletionRate >= 80 ? "可以推进到下一阶段，并增加综合练习。" : "保持当前阶段，优先完成未完成任务。");
        if (!report.weakPoints().isEmpty()) {
            adjustments.add("围绕最高频薄弱点生成 3-5 道专项题，并在复习页加入间隔复习。");
        }
        adjustments.add("每次上传新资料后重新生成一组资料增强题，校验计划是否需要调整。");

        List<String> nextActions = new ArrayList<>();
        nextActions.add("完成当前阶段剩余任务。");
        nextActions.add(report.wrongQuestionCount() > 0 ? "重做错题并同步今日复习。" : "追加更高难度或不同题型的测验。");
        nextActions.add("刷新学习报告，确认趋势变化。");

        return new PlanAdjustmentResponse(planId, taskCompletionRate, report.accuracyRate(), risks, adjustments, nextActions);
    }

    @Transactional
    public PlanResponse applyAdjustment(Long planId, ApplyPlanAdjustmentRequest request) {
        LearningPlan plan = ensurePlanEntity(planId);
        PlanAdjustmentResponse adjustment = adjustments(planId);
        List<String> tasks = request.tasks() == null || request.tasks().isEmpty()
                ? adjustment.nextActions()
                : request.tasks();
        String stageName = request.stageName() == null || request.stageName().isBlank()
                ? "动态调整：" + java.time.LocalDate.now()
                : request.stageName();
        try {
            JsonNode existing = objectMapper.readTree(plan.getStages());
            com.fasterxml.jackson.databind.node.ArrayNode stages = existing.isArray()
                    ? (com.fasterxml.jackson.databind.node.ArrayNode) existing
                    : objectMapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ObjectNode stage = objectMapper.createObjectNode();
            stage.put("name", stageName);
            stage.put("focus", "基于最新任务完成、测验和复习结果自动加入的调整阶段。");
            stage.put("duration", "1 周");
            com.fasterxml.jackson.databind.node.ArrayNode taskNodes = objectMapper.createArrayNode();
            tasks.forEach(taskNodes::add);
            stage.set("tasks", taskNodes);
            stages.add(stage);
            plan.updateStages(objectMapper.writeValueAsString(stages));
            syncTaskRecords(plan);
            return PlanResponse.from(plan, ensureDirection(plan.getDirectionId()));
        } catch (Exception ex) {
            throw new BusinessException("PLAN_ADJUST_FAILED", "Unable to apply plan adjustment");
        }
    }

    @Transactional
    public void delete(Long planId) {
        LearningPlan plan = ensurePlanEntity(planId);
        List<KnowledgeDocument> documents = documentRepository.findByPlanIdOrderByUploadedAtDesc(planId);
        for (KnowledgeDocument document : documents) {
            documentService.delete(planId, document.getId());
        }

        List<Long> questionIds = questionRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                .stream()
                .map(Question::getId)
                .toList();
        if (!questionIds.isEmpty()) {
            answerRecordRepository.deleteByQuestionIdIn(questionIds);
        }
        questionRepository.deleteByPlanId(planId);
        qaRecordRepository.deleteByPlanId(planId);
        reviewRecordRepository.deleteByPlanId(planId);
        taskRecordRepository.deleteByPlanId(planId);
        agentTaskRepository.deleteByPlanId(planId);
        summaryCardService.deleteByPlan(planId);
        planRepository.delete(plan);
    }

    private LearningDirection ensureDirection(Long directionId) {
        return directionRepository.findById(directionId)
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning direction not found"));
    }

    private LearningPlan ensurePlanEntity(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        ensureDirection(plan.getDirectionId());
        return plan;
    }

    private String nextShareCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String code = randomShareCode();
            if (planRepository.findByShareCode(code).isEmpty()) {
                return code;
            }
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private String randomShareCode() {
        StringBuilder builder = new StringBuilder(16);
        for (int index = 0; index < 16; index++) {
            builder.append(SHARE_ALPHABET[SHARE_RANDOM.nextInt(SHARE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private void syncTaskRecords(LearningPlan plan) {
        List<TaskSeed> seeds = parseTaskSeeds(plan);
        for (TaskSeed seed : seeds) {
            taskRecordRepository
                    .findByPlanIdAndUserIdAndStageIndexAndTaskIndex(
                            plan.getId(),
                            AuthContext.currentUserId(),
                            seed.stageIndex(),
                            seed.taskIndex()
                    )
                    .orElseGet(() -> taskRecordRepository.save(new PlanTaskRecord(
                            plan.getId(),
                            AuthContext.currentUserId(),
                            seed.stageIndex(),
                            seed.taskIndex(),
                            seed.taskText()
                    )));
        }
    }

    private List<TaskSeed> parseTaskSeeds(LearningPlan plan) {
        if (plan.getStages() == null || plan.getStages().isBlank()) {
            return List.of();
        }
        try {
            JsonNode stages = objectMapper.readTree(plan.getStages());
            List<TaskSeed> seeds = new ArrayList<>();
            for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
                JsonNode stage = stages.get(stageIndex);
                JsonNode tasks = stage.path("tasks");
                if (!tasks.isArray()) {
                    tasks = objectMapper.createArrayNode();
                }
                int nextTaskIndex = 0;
                for (; nextTaskIndex < tasks.size(); nextTaskIndex++) {
                    seeds.add(new TaskSeed(stageIndex, nextTaskIndex, tasks.get(nextTaskIndex).asText()));
                }
                JsonNode units = stage.path("units");
                if (!units.isArray()) {
                    continue;
                }
                for (JsonNode unit : units) {
                    JsonNode points = unit.path("knowledgePoints");
                    if (!points.isArray()) {
                        continue;
                    }
                    for (JsonNode point : points) {
                        String title = point.path("title").asText("");
                        if (!title.isBlank()) {
                            String unitName = unit.path("name").asText("单元");
                            seeds.add(new TaskSeed(stageIndex, nextTaskIndex++, "学习知识点：" + unitName + " / " + title));
                        }
                    }
                }
            }
            return seeds;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void refreshCurrentStage(LearningPlan plan) {
        List<PlanTaskRecord> records = taskRecordRepository
                .findByPlanIdAndUserIdOrderByStageIndexAscTaskIndexAsc(plan.getId(), AuthContext.currentUserId());
        int currentStage = records.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getCompleted()))
                .map(PlanTaskRecord::getStageIndex)
                .findFirst()
                .orElse(records.stream().map(PlanTaskRecord::getStageIndex).max(Integer::compareTo).orElse(0));
        plan.updateCurrentStageIndex(currentStage);
    }

    private record TaskSeed(int stageIndex, int taskIndex, String taskText) {
    }
}
