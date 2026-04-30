package com.xagentstudy.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.quiz.AnswerRecord;
import com.xagentstudy.quiz.AnswerRecordRepository;
import com.xagentstudy.quiz.Question;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.rag.chunk.SourceLabelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningReportService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final ObjectMapper objectMapper;
    private final SourceLabelService sourceLabelService;

    public LearningReportService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            QuestionRepository questionRepository,
            AnswerRecordRepository answerRecordRepository,
            PlanTaskRecordRepository taskRecordRepository,
            ObjectMapper objectMapper,
            SourceLabelService sourceLabelService
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.questionRepository = questionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.taskRecordRepository = taskRecordRepository;
        this.objectMapper = objectMapper;
        this.sourceLabelService = sourceLabelService;
    }

    @Transactional(readOnly = true)
    public LearningReportResponse getReport(Long planId) {
        ensurePlan(planId);
        List<Question> questions = questionRepository.findByPlanIdOrderByCreatedAtDesc(planId);
        if (questions.isEmpty()) {
            return emptyReport(planId);
        }

        Map<Long, Question> questionMap = new HashMap<>();
        List<Long> questionIds = new ArrayList<>();
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
            questionIds.add(question.getId());
        }

        List<AnswerRecord> attempts = answerRecordRepository
                .findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(questionIds, AuthContext.currentUserId());
        Map<Long, AnswerRecord> latestByQuestion = new LinkedHashMap<>();
        for (AnswerRecord attempt : attempts) {
            latestByQuestion.putIfAbsent(attempt.getQuestionId(), attempt);
        }

        List<WrongQuestionResponse> wrongQuestions = new ArrayList<>();
        Map<String, Integer> weakPointCount = new HashMap<>();
        int correctQuestionCount = 0;
        for (Map.Entry<Long, AnswerRecord> entry : latestByQuestion.entrySet()) {
            Question question = questionMap.get(entry.getKey());
            if (question == null) {
                continue;
            }
            AnswerRecord attempt = entry.getValue();
            if (Boolean.TRUE.equals(attempt.getCorrect())) {
                correctQuestionCount++;
                continue;
            }
            List<String> points = parseStringList(question.getKnowledgePoints());
            points.forEach(point -> weakPointCount.merge(point, 1, Integer::sum));
            wrongQuestions.add(new WrongQuestionResponse(
                    question.getId(),
                    question.getStem(),
                    attempt.getUserAnswer(),
                    question.getStandardAnswer(),
                    question.getExplanation(),
                    points,
                    question.getSourceScope(),
                    sourceLabelService.label(question.getSourceScope()),
                    attempt.getAnsweredAt()
            ));
        }

        List<String> weakPoints = weakPointCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .map(Map.Entry::getKey)
                .toList();
        List<String> suggestions = buildSuggestions(wrongQuestions, weakPoints, latestByQuestion.size(), questions.size());
        List<String> weaknessReasons = buildWeaknessReasons(weakPointCount, wrongQuestions);
        int answeredQuestionCount = latestByQuestion.size();
        double accuracyRate = answeredQuestionCount == 0 ? 0 : Math.round(correctQuestionCount * 1000.0 / answeredQuestionCount) / 10.0;
        double taskCompletionRate = taskCompletionRate(planId);
        int masteryScore = masteryScore(taskCompletionRate, accuracyRate, answeredQuestionCount, questions.size());

        return new LearningReportResponse(
                planId,
                questions.size(),
                attempts.size(),
                answeredQuestionCount,
                correctQuestionCount,
                wrongQuestions.size(),
                accuracyRate,
                taskCompletionRate,
                masteryScore,
                weakPoints,
                weaknessReasons,
                buildWeakPointSources(wrongQuestions),
                wrongQuestions.stream().limit(8).toList(),
                suggestions,
                buildNextActions(taskCompletionRate, accuracyRate, wrongQuestions),
                buildTrend(attempts)
        );
    }

    private LearningReportResponse emptyReport(Long planId) {
        return new LearningReportResponse(
                planId,
                0,
                0,
                0,
                0,
                0,
                0,
                taskCompletionRate(planId),
                (int) Math.round(taskCompletionRate(planId) * 0.4),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("先进入测验页生成一组通用题；上传资料后会自动升级为资料增强题。"),
                List.of("完成当前阶段至少 1 个任务。", "生成一组测验建立初始掌握度。"),
                List.of()
        );
    }

    private List<String> buildSuggestions(
            List<WrongQuestionResponse> wrongQuestions,
            List<String> weakPoints,
            int answeredQuestionCount,
            int questionCount
    ) {
        List<String> suggestions = new ArrayList<>();
        if (answeredQuestionCount < questionCount) {
            suggestions.add("先完成剩余未作答题目，让报告覆盖完整题库。");
        }
        if (!weakPoints.isEmpty()) {
            suggestions.add("优先复习：" + String.join("、", weakPoints.subList(0, Math.min(3, weakPoints.size()))) + "。");
        }
        if (!wrongQuestions.isEmpty()) {
            suggestions.add("重做最近错题，并对照解析补一条自己的解释。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前题目掌握较稳，可以追加生成更高难度题目。");
        }
        return suggestions;
    }

    private List<String> buildWeaknessReasons(Map<String, Integer> weakPointCount, List<WrongQuestionResponse> wrongQuestions) {
        if (weakPointCount.isEmpty()) {
            return List.of("暂无明显薄弱点；继续通过更高难度题目验证迁移能力。");
        }
        return weakPointCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> entry.getKey() + " 出现 " + entry.getValue() + " 次错题，建议先复述概念再重做相关题。")
                .toList();
    }

    private List<WeakPointSourceResponse> buildWeakPointSources(List<WrongQuestionResponse> wrongQuestions) {
        Map<String, WeakPointSourceAccumulator> grouped = new HashMap<>();
        for (WrongQuestionResponse question : wrongQuestions) {
            for (String point : question.knowledgePoints()) {
                String key = point + "\n" + question.sourceScope();
                grouped.computeIfAbsent(key, ignored -> new WeakPointSourceAccumulator(point, question.sourceScope(), question.sourceLabel()))
                        .increment();
            }
        }
        return grouped.values()
                .stream()
                .sorted(Comparator.comparingInt(WeakPointSourceAccumulator::wrongCount).reversed())
                .limit(8)
                .map(item -> new WeakPointSourceResponse(item.knowledgePoint, item.sourceScope, item.sourceLabel, item.wrongCount))
                .toList();
    }

    private List<String> buildNextActions(double taskCompletionRate, double accuracyRate, List<WrongQuestionResponse> wrongQuestions) {
        List<String> actions = new ArrayList<>();
        if (taskCompletionRate < 80) {
            actions.add("优先补齐未完成计划任务，避免只做题不推进阶段任务。");
        }
        if (!wrongQuestions.isEmpty()) {
            actions.add("进入复习页同步薄弱点，并按间隔复习完成今日清单。");
        }
        if (accuracyRate >= 80) {
            actions.add("追加代码题、案例题或面试题，检验迁移应用能力。");
        } else {
            actions.add("生成 3 道同知识点低难度题，先稳定正确率。");
        }
        return actions;
    }

    private List<StudyTrendPoint> buildTrend(List<AnswerRecord> attempts) {
        Map<String, StudyTrendPointAccumulator> grouped = new LinkedHashMap<>();
        attempts.stream()
                .sorted(Comparator.comparing(AnswerRecord::getAnsweredAt))
                .forEach(attempt -> {
                    String day = attempt.getAnsweredAt().toLocalDate().toString();
                    grouped.computeIfAbsent(day, ignored -> new StudyTrendPointAccumulator())
                            .add(Boolean.TRUE.equals(attempt.getCorrect()));
                });
        return grouped.entrySet()
                .stream()
                .map(entry -> new StudyTrendPoint(entry.getKey(), entry.getValue().attempts, entry.getValue().correct))
                .toList();
    }

    private double taskCompletionRate(Long planId) {
        long total = taskRecordRepository.countByPlanIdAndUserId(planId, AuthContext.currentUserId());
        if (total == 0) {
            return 0;
        }
        long completed = taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrue(planId, AuthContext.currentUserId());
        return Math.round(completed * 1000.0 / total) / 10.0;
    }

    private int masteryScore(double taskCompletionRate, double accuracyRate, int answeredQuestionCount, int questionCount) {
        double answerCoverage = questionCount == 0 ? 0 : answeredQuestionCount * 100.0 / questionCount;
        return (int) Math.round(taskCompletionRate * 0.35 + accuracyRate * 0.45 + answerCoverage * 0.20);
    }

    private List<String> parseStringList(String value) {
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

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private static class StudyTrendPointAccumulator {
        private int attempts;
        private int correct;

        private void add(boolean correctAnswer) {
            attempts++;
            if (correctAnswer) {
                correct++;
            }
        }
    }

    private static class WeakPointSourceAccumulator {
        private final String knowledgePoint;
        private final String sourceScope;
        private final String sourceLabel;
        private int wrongCount;

        private WeakPointSourceAccumulator(String knowledgePoint, String sourceScope, String sourceLabel) {
            this.knowledgePoint = knowledgePoint;
            this.sourceScope = sourceScope;
            this.sourceLabel = sourceLabel;
        }

        private void increment() {
            wrongCount++;
        }

        private int wrongCount() {
            return wrongCount;
        }
    }
}
