package com.xagentstudy.outcome;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.checkin.DailyCheckinRepository;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.outcome.range.LearningOutcomeRange;
import com.xagentstudy.outcome.response.LearningOutcomeResponse;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.task.PlanTaskRecord;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.quiz.AnswerRecord;
import com.xagentstudy.quiz.AnswerRecordRepository;
import com.xagentstudy.quiz.Question;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.report.LearningReportResponse;
import com.xagentstudy.report.LearningReportService;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import com.xagentstudy.summary.SummaryCard;
import com.xagentstudy.summary.SummaryCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LearningOutcomeService {
    private final DailyCheckinRepository dailyCheckinRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final QuestionRepository questionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final SummaryCardRepository summaryCardRepository;
    private final LearningReportService learningReportService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public LearningOutcomeService(
            DailyCheckinRepository dailyCheckinRepository,
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            PlanTaskRecordRepository taskRecordRepository,
            AnswerRecordRepository answerRecordRepository,
            QuestionRepository questionRepository,
            ReviewRecordRepository reviewRecordRepository,
            SummaryCardRepository summaryCardRepository,
            LearningReportService learningReportService,
            ObjectMapper objectMapper
    ) {
        this(
                dailyCheckinRepository,
                directionRepository,
                planRepository,
                taskRecordRepository,
                answerRecordRepository,
                questionRepository,
                reviewRecordRepository,
                summaryCardRepository,
                learningReportService,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    LearningOutcomeService(
            DailyCheckinRepository dailyCheckinRepository,
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            PlanTaskRecordRepository taskRecordRepository,
            AnswerRecordRepository answerRecordRepository,
            QuestionRepository questionRepository,
            ReviewRecordRepository reviewRecordRepository,
            SummaryCardRepository summaryCardRepository,
            LearningReportService learningReportService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.dailyCheckinRepository = dailyCheckinRepository;
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.taskRecordRepository = taskRecordRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.questionRepository = questionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.summaryCardRepository = summaryCardRepository;
        this.learningReportService = learningReportService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearningOutcomeResponse getOutcomes(String rangeCode, LocalDate startDate, LocalDate endDate) {
        Long userId = AuthContext.currentUserId();
        LearningOutcomeRange range = LearningOutcomeRange.resolve(rangeCode, startDate, endDate, LocalDate.now(clock));
        ZoneId zoneId = clock.getZone();
        OffsetDateTime startAt = range.startAt(zoneId);
        OffsetDateTime endExclusiveAt = range.endExclusiveAt(zoneId);

        List<LearningDirection> directions = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId);
        List<Long> directionIds = directions.stream().map(LearningDirection::getId).toList();
        List<LearningPlan> plans = directionIds.isEmpty()
                ? List.of()
                : planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds);
        List<Long> planIds = plans.stream().map(LearningPlan::getId).toList();

        List<PlanTaskRecord> taskRecords = planIds.isEmpty()
                ? List.of()
                : taskRecordRepository.findByUserIdAndPlanIdInOrderByPlanIdAscStageIndexAscTaskIndexAsc(userId, planIds);
        List<AnswerRecord> attemptsInRange = answerRecordRepository.findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtAsc(userId, startAt, endExclusiveAt);
        List<ReviewRecord> reviewRecords = planIds.isEmpty()
                ? List.of()
                : reviewRecordRepository.findByUserIdAndPlanIdInOrderByRecommendedAtDesc(userId, planIds);
        List<SummaryCard> summaryCards = summaryCardRepository.findTop5ByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startAt, endExclusiveAt);

        Map<Long, List<LearningPlan>> plansByDirectionId = groupPlansByDirectionId(plans);
        Map<Long, List<PlanTaskRecord>> taskRecordsByPlanId = groupTasksByPlanId(taskRecords);
        Map<Long, LearningPlan> latestPlanByDirectionId = latestPlanByDirectionId(plans);
        Map<Long, LearningReportResponse> reportByPlanId = loadReports(latestPlanByDirectionId.values());

        LearningOutcomeResponse.Overview overview = buildOverview(
                userId,
                range,
                startAt,
                endExclusiveAt,
                taskRecords,
                attemptsInRange,
                reviewRecords
        );
        List<LearningOutcomeResponse.DirectionSnapshot> directionSnapshots = buildDirectionSnapshots(
                userId,
                directions,
                plansByDirectionId,
                latestPlanByDirectionId,
                taskRecordsByPlanId,
                reviewRecords,
                reportByPlanId,
                startAt,
                endExclusiveAt
        );
        List<LearningOutcomeResponse.TrendPoint> trends = buildTrends(range, taskRecords, attemptsInRange, reviewRecords);
        List<LearningOutcomeResponse.Highlight> highlights = buildHighlights(summaryCards, directionSnapshots);
        List<String> suggestions = buildSuggestions(taskRecords, reviewRecords, reportByPlanId, directionSnapshots);
        LearningOutcomeResponse.Poster poster = buildPoster(range, overview, directionSnapshots, highlights);

        return new LearningOutcomeResponse(
                range.code(),
                range.label(),
                range.startDate(),
                range.endDate(),
                overview,
                directionSnapshots,
                trends,
                highlights,
                suggestions,
                poster
        );
    }

    private LearningOutcomeResponse.Overview buildOverview(
            Long userId,
            LearningOutcomeRange range,
            OffsetDateTime startAt,
            OffsetDateTime endExclusiveAt,
            List<PlanTaskRecord> taskRecords,
            List<AnswerRecord> attemptsInRange,
            List<ReviewRecord> reviewRecords
    ) {
        int learningDays = dailyCheckinRepository
                .findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, range.startDate(), range.endDate())
                .size();
        long completedTaskCount = taskRecordRepository.countByUserIdAndCompletedTrueAndCompletedAtBetween(userId, startAt, endExclusiveAt);
        long completedPlanCount = countCompletedPlans(taskRecords, startAt, endExclusiveAt);
        long completedReviewCount = reviewRecords.stream()
                .filter(record -> record.getCompletedAt() != null)
                .filter(record -> withinRange(record.getCompletedAt(), startAt, endExclusiveAt))
                .count();

        return new LearningOutcomeResponse.Overview(
                learningDays,
                completedTaskCount,
                completedPlanCount,
                attemptsInRange.size(),
                countMasteredKnowledgePoints(attemptsInRange),
                completedReviewCount
        );
    }

    private List<LearningOutcomeResponse.DirectionSnapshot> buildDirectionSnapshots(
            Long userId,
            List<LearningDirection> directions,
            Map<Long, List<LearningPlan>> plansByDirectionId,
            Map<Long, LearningPlan> latestPlanByDirectionId,
            Map<Long, List<PlanTaskRecord>> taskRecordsByPlanId,
            List<ReviewRecord> reviewRecords,
            Map<Long, LearningReportResponse> reportByPlanId,
            OffsetDateTime startAt,
            OffsetDateTime endExclusiveAt
    ) {
        List<LearningOutcomeResponse.DirectionSnapshot> snapshots = new ArrayList<>();
        for (LearningDirection direction : directions) {
            List<LearningPlan> directionPlans = plansByDirectionId.getOrDefault(direction.getId(), List.of());
            LearningPlan latestPlan = latestPlanByDirectionId.get(direction.getId());
            LearningReportResponse report = latestPlan == null ? null : reportByPlanId.get(latestPlan.getId());
            List<PlanTaskRecord> latestPlanTasks = latestPlan == null
                    ? List.of()
                    : taskRecordsByPlanId.getOrDefault(latestPlan.getId(), List.of());
            long completedReviews = reviewRecords.stream()
                    .filter(record -> directionPlans.stream().anyMatch(plan -> plan.getId().equals(record.getPlanId())))
                    .filter(record -> record.getCompletedAt() != null)
                    .filter(record -> withinRange(record.getCompletedAt(), startAt, endExclusiveAt))
                    .count();
            snapshots.add(new LearningOutcomeResponse.DirectionSnapshot(
                    direction.getId(),
                    direction.getName(),
                    directionPlans.size(),
                    completionRate(latestPlanTasks),
                    report == null ? 0 : report.accuracyRate(),
                    completedReviews,
                    latestPlan == null ? List.of() : collectStrongPoints(userId, latestPlan.getId()),
                    report == null ? List.of() : limit(report.weakPoints(), 3)
            ));
        }
        return snapshots;
    }

    private List<LearningOutcomeResponse.TrendPoint> buildTrends(
            LearningOutcomeRange range,
            List<PlanTaskRecord> taskRecords,
            List<AnswerRecord> attemptsInRange,
            List<ReviewRecord> reviewRecords
    ) {
        Map<LocalDate, TrendAccumulator> trendMap = new LinkedHashMap<>();
        LocalDate cursor = "ALL".equals(range.code())
                ? earliestTrendDate(taskRecords, attemptsInRange, reviewRecords).orElse(range.endDate())
                : range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            trendMap.put(cursor, new TrendAccumulator());
            cursor = cursor.plusDays(1);
        }

        for (PlanTaskRecord taskRecord : taskRecords) {
            if (Boolean.TRUE.equals(taskRecord.getCompleted()) && taskRecord.getCompletedAt() != null) {
                TrendAccumulator accumulator = trendMap.get(taskRecord.getCompletedAt().toLocalDate());
                if (accumulator != null) {
                    accumulator.completedTasks++;
                }
            }
        }
        for (AnswerRecord attempt : attemptsInRange) {
            TrendAccumulator accumulator = trendMap.get(attempt.getAnsweredAt().toLocalDate());
            if (accumulator == null) {
                continue;
            }
            accumulator.quizAttempts++;
            if (Boolean.TRUE.equals(attempt.getCorrect())) {
                accumulator.correctAnswers++;
            }
        }
        for (ReviewRecord reviewRecord : reviewRecords) {
            if (reviewRecord.getCompletedAt() == null) {
                continue;
            }
            TrendAccumulator accumulator = trendMap.get(reviewRecord.getCompletedAt().toLocalDate());
            if (accumulator != null) {
                accumulator.completedReviews++;
            }
        }

        return trendMap.entrySet()
                .stream()
                .map(entry -> new LearningOutcomeResponse.TrendPoint(
                        entry.getKey(),
                        entry.getValue().completedTasks,
                        entry.getValue().quizAttempts,
                        entry.getValue().correctAnswers,
                        entry.getValue().completedReviews
                ))
                .toList();
    }

    private List<LearningOutcomeResponse.Highlight> buildHighlights(
            List<SummaryCard> summaryCards,
            List<LearningOutcomeResponse.DirectionSnapshot> directionSnapshots
    ) {
        List<LearningOutcomeResponse.Highlight> highlights = summaryCards.stream()
                .map(card -> new LearningOutcomeResponse.Highlight(
                        "SUMMARY_CARD",
                        card.getId(),
                        card.getTitle(),
                        card.getSourceDocumentName() == null || card.getSourceDocumentName().isBlank()
                                ? "学习摘要卡"
                                : card.getSourceDocumentName(),
                        fallback(card.getSummary(), "沉淀了一条可复用的学习结论。")
                ))
                .limit(3)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        for (LearningOutcomeResponse.DirectionSnapshot snapshot : directionSnapshots) {
            if (highlights.size() >= 3) {
                break;
            }
            String summary;
            if (!snapshot.strengths().isEmpty()) {
                summary = "优势知识点：" + String.join("、", snapshot.strengths());
            } else if (!snapshot.weakPoints().isEmpty()) {
                summary = "优先补强：" + String.join("、", snapshot.weakPoints());
            } else {
                summary = "已形成一份当前阶段学习快照。";
            }
            highlights.add(new LearningOutcomeResponse.Highlight(
                    "DIRECTION",
                    snapshot.directionId(),
                    snapshot.directionName(),
                    "任务完成率 " + formatRate(snapshot.taskCompletionRate()) + "，正确率 " + formatRate(snapshot.accuracyRate()),
                    summary
            ));
        }
        return highlights;
    }

    private List<String> buildSuggestions(
            List<PlanTaskRecord> taskRecords,
            List<ReviewRecord> reviewRecords,
            Map<Long, LearningReportResponse> reportByPlanId,
            List<LearningOutcomeResponse.DirectionSnapshot> directionSnapshots
    ) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        List<String> pendingTasks = taskRecords.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getCompleted()))
                .map(PlanTaskRecord::getTaskText)
                .filter(text -> text != null && !text.isBlank())
                .limit(2)
                .toList();
        if (!pendingTasks.isEmpty()) {
            suggestions.add("优先完成未结束任务：" + String.join("；", pendingTasks) + "。");
        }

        long pendingReviews = reviewRecords.stream().filter(record -> !Boolean.TRUE.equals(record.getCompleted())).count();
        if (pendingReviews > 0) {
            suggestions.add("还有 " + pendingReviews + " 条复习待完成，建议先清掉今日复习清单。");
        }

        for (LearningReportResponse report : reportByPlanId.values()) {
            if (!report.weakPoints().isEmpty()) {
                suggestions.add("优先补强薄弱点：" + String.join("、", limit(report.weakPoints(), 3)) + "。");
            }
            if (!report.nextActions().isEmpty()) {
                suggestions.add(report.nextActions().get(0));
            }
            if (!report.reviewSuggestions().isEmpty()) {
                suggestions.add(report.reviewSuggestions().get(0));
            }
            if (suggestions.size() >= 4) {
                break;
            }
        }

        if (suggestions.isEmpty() && !directionSnapshots.isEmpty()) {
            LearningOutcomeResponse.DirectionSnapshot topDirection = directionSnapshots.get(0);
            suggestions.add("继续围绕 " + topDirection.directionName() + " 增加一轮练习，保持当前学习节奏。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("先完成一次打卡、一道题和一条复习记录，系统会生成更完整的成果快照。");
        }
        return suggestions.stream().limit(4).toList();
    }

    private LearningOutcomeResponse.Poster buildPoster(
            LearningOutcomeRange range,
            LearningOutcomeResponse.Overview overview,
            List<LearningOutcomeResponse.DirectionSnapshot> directionSnapshots,
            List<LearningOutcomeResponse.Highlight> highlights
    ) {
        LearningOutcomeResponse.DirectionSnapshot topDirection = directionSnapshots.isEmpty() ? null : directionSnapshots.get(0);
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (topDirection != null) {
            keywords.add(topDirection.directionName());
            keywords.addAll(limit(topDirection.strengths(), 3));
            keywords.addAll(limit(topDirection.weakPoints(), 2));
        }
        if (keywords.isEmpty()) {
            keywords.add("学习积累");
            keywords.add("阶段复盘");
        }

        List<String> posterHighlights = new ArrayList<>();
        posterHighlights.add(range.label());
        posterHighlights.add("完成任务 " + overview.completedTaskCount() + " 项");
        posterHighlights.add("答题 " + overview.quizAttemptCount() + " 次");
        if (!highlights.isEmpty()) {
            posterHighlights.add(highlights.get(0).title());
        }

        String title = topDirection == null
                ? "学习成果海报"
                : topDirection.directionName() + " 学习成果";
        String subtitle = range.label()
                + " 内累计学习 "
                + overview.learningDays()
                + " 天，完成复习 "
                + overview.completedReviewCount()
                + " 次。";

        return new LearningOutcomeResponse.Poster(
                title,
                subtitle,
                keywords.stream().limit(6).toList(),
                posterHighlights.stream().limit(4).toList()
        );
    }

    private Map<Long, LearningReportResponse> loadReports(Collection<LearningPlan> latestPlans) {
        Map<Long, LearningReportResponse> reportByPlanId = new HashMap<>();
        for (LearningPlan plan : latestPlans) {
            reportByPlanId.put(plan.getId(), learningReportService.getReport(plan.getId()));
        }
        return reportByPlanId;
    }

    private Map<Long, List<LearningPlan>> groupPlansByDirectionId(List<LearningPlan> plans) {
        Map<Long, List<LearningPlan>> plansByDirectionId = new LinkedHashMap<>();
        for (LearningPlan plan : plans) {
            plansByDirectionId.computeIfAbsent(plan.getDirectionId(), ignored -> new ArrayList<>()).add(plan);
        }
        return plansByDirectionId;
    }

    private Map<Long, LearningPlan> latestPlanByDirectionId(List<LearningPlan> plans) {
        Map<Long, LearningPlan> latestPlanByDirectionId = new LinkedHashMap<>();
        for (LearningPlan plan : plans) {
            latestPlanByDirectionId.putIfAbsent(plan.getDirectionId(), plan);
        }
        return latestPlanByDirectionId;
    }

    private Map<Long, List<PlanTaskRecord>> groupTasksByPlanId(List<PlanTaskRecord> taskRecords) {
        Map<Long, List<PlanTaskRecord>> taskRecordsByPlanId = new LinkedHashMap<>();
        for (PlanTaskRecord taskRecord : taskRecords) {
            taskRecordsByPlanId.computeIfAbsent(taskRecord.getPlanId(), ignored -> new ArrayList<>()).add(taskRecord);
        }
        return taskRecordsByPlanId;
    }

    private long countCompletedPlans(List<PlanTaskRecord> taskRecords, OffsetDateTime startAt, OffsetDateTime endExclusiveAt) {
        return groupTasksByPlanId(taskRecords).values().stream()
                .filter(records -> !records.isEmpty())
                .filter(records -> records.stream().allMatch(record -> Boolean.TRUE.equals(record.getCompleted()) && record.getCompletedAt() != null))
                .filter(records -> records.stream()
                        .map(PlanTaskRecord::getCompletedAt)
                        .max(Comparator.naturalOrder())
                        .filter(completedAt -> withinRange(completedAt, startAt, endExclusiveAt))
                        .isPresent())
                .count();
    }

    private int countMasteredKnowledgePoints(List<AnswerRecord> attemptsInRange) {
        List<Long> correctQuestionIds = attemptsInRange.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCorrect()))
                .map(AnswerRecord::getQuestionId)
                .distinct()
                .toList();
        if (correctQuestionIds.isEmpty()) {
            return 0;
        }

        Set<String> mastered = new LinkedHashSet<>();
        for (Question question : questionRepository.findAllById(correctQuestionIds)) {
            mastered.addAll(parseStringList(question.getKnowledgePoints()));
        }
        return mastered.size();
    }

    private List<String> collectStrongPoints(Long userId, Long planId) {
        List<Question> questions = questionRepository.findByPlanIdOrderByCreatedAtDesc(planId);
        if (questions.isEmpty()) {
            return List.of();
        }

        Map<Long, Question> questionMap = new LinkedHashMap<>();
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
        }

        Map<Long, AnswerRecord> latestByQuestion = new LinkedHashMap<>();
        List<Long> questionIds = new ArrayList<>(questionMap.keySet());
        for (AnswerRecord attempt : answerRecordRepository.findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(questionIds, userId)) {
            latestByQuestion.putIfAbsent(attempt.getQuestionId(), attempt);
        }

        Map<String, Integer> scoreByKnowledgePoint = new HashMap<>();
        for (Map.Entry<Long, AnswerRecord> entry : latestByQuestion.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue().getCorrect())) {
                continue;
            }
            Question question = questionMap.get(entry.getKey());
            if (question == null) {
                continue;
            }
            for (String point : parseStringList(question.getKnowledgePoints())) {
                scoreByKnowledgePoint.merge(point, 1, Integer::sum);
            }
        }

        return scoreByKnowledgePoint.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();
    }

    private List<String> parseStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private double completionRate(List<PlanTaskRecord> taskRecords) {
        if (taskRecords.isEmpty()) {
            return 0;
        }
        long completed = taskRecords.stream().filter(record -> Boolean.TRUE.equals(record.getCompleted())).count();
        return Math.round(completed * 1000.0 / taskRecords.size()) / 10.0;
    }

    private boolean withinRange(OffsetDateTime value, OffsetDateTime startAt, OffsetDateTime endExclusiveAt) {
        return !value.isBefore(startAt) && value.isBefore(endExclusiveAt);
    }

    private java.util.Optional<LocalDate> earliestTrendDate(
            List<PlanTaskRecord> taskRecords,
            List<AnswerRecord> attemptsInRange,
            List<ReviewRecord> reviewRecords
    ) {
        LocalDate earliest = null;
        for (PlanTaskRecord taskRecord : taskRecords) {
            if (taskRecord.getCompletedAt() != null) {
                LocalDate candidate = taskRecord.getCompletedAt().toLocalDate();
                earliest = earliest == null || candidate.isBefore(earliest) ? candidate : earliest;
            }
        }
        for (AnswerRecord attempt : attemptsInRange) {
            LocalDate candidate = attempt.getAnsweredAt().toLocalDate();
            earliest = earliest == null || candidate.isBefore(earliest) ? candidate : earliest;
        }
        for (ReviewRecord reviewRecord : reviewRecords) {
            if (reviewRecord.getCompletedAt() != null) {
                LocalDate candidate = reviewRecord.getCompletedAt().toLocalDate();
                earliest = earliest == null || candidate.isBefore(earliest) ? candidate : earliest;
            }
        }
        return java.util.Optional.ofNullable(earliest);
    }

    private List<String> limit(List<String> values, int maxSize) {
        return values.stream().limit(maxSize).toList();
    }

    private String formatRate(double rate) {
        return Math.round(rate * 10.0) / 10.0 + "%";
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private static final class TrendAccumulator {
        private int completedTasks;
        private int quizAttempts;
        private int correctAnswers;
        private int completedReviews;
    }
}
