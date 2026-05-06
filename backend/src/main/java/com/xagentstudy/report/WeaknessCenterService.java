package com.xagentstudy.report;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WeaknessCenterService {
    private final LearningReportService reportService;
    private final ReviewRecordRepository reviewRecordRepository;

    public WeaknessCenterService(LearningReportService reportService, ReviewRecordRepository reviewRecordRepository) {
        this.reportService = reportService;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    @Transactional(readOnly = true)
    public WeaknessCenterResponse getCenter(Long planId) {
        LearningReportResponse report = reportService.getReport(planId);
        Map<String, ReviewRecord> reviewMap = new LinkedHashMap<>();
        for (ReviewRecord record : reviewRecordRepository.findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(planId, AuthContext.currentUserId())) {
            reviewMap.put(record.getKnowledgePoint(), record);
        }

        Map<String, WeaknessAccumulator> grouped = new LinkedHashMap<>();
        for (WeakPointSourceResponse source : report.weakPointSources()) {
            grouped.computeIfAbsent(source.knowledgePoint(), WeaknessAccumulator::new)
                    .addSource(source.sourceLabel(), source.wrongCount());
        }
        for (WrongQuestionResponse question : report.wrongQuestions()) {
            for (String point : question.knowledgePoints()) {
                grouped.computeIfAbsent(point, WeaknessAccumulator::new)
                        .addQuestion(question.questionId());
            }
        }
        for (String point : report.weakPoints()) {
            grouped.computeIfAbsent(point, WeaknessAccumulator::new);
        }

        List<WeaknessItemResponse> items = grouped.values().stream()
                .map(item -> toItem(item, reviewMap.get(item.knowledgePoint), report))
                .toList();
        int mastered = (int) items.stream().filter(item -> item.status().equals("已掌握")).count();
        int repairing = (int) items.stream().filter(item -> item.status().equals("修复中")).count();
        int totalWrong = items.stream().mapToInt(WeaknessItemResponse::wrongCount).sum();
        return new WeaknessCenterResponse(
                planId,
                new WeaknessSummaryResponse(items.size() - mastered, repairing, mastered, totalWrong),
                items,
                report.reviewSuggestions()
        );
    }

    private WeaknessItemResponse toItem(WeaknessAccumulator item, ReviewRecord review, LearningReportResponse report) {
        int masteryScore = review == null ? 0 : Optional.ofNullable(review.getMasteryScore()).orElse(0);
        String status;
        if (review != null && Boolean.TRUE.equals(review.getCompleted()) && masteryScore >= 60) {
            status = "已掌握";
        } else if (review != null) {
            status = "修复中";
        } else {
            status = "未修复";
        }
        String reason = report.weaknessReasons().stream()
                .filter(value -> value.startsWith(item.knowledgePoint))
                .findFirst()
                .orElse("来自错题或报告聚合，建议先复述概念，再完成针对练习。");
        return new WeaknessItemResponse(
                item.knowledgePoint,
                status,
                item.wrongCount,
                masteryScore,
                reason,
                item.sources,
                item.questionIds,
                List.of("去复习", "重做错题", "生成针对练习", "查看报告")
        );
    }

    private static class WeaknessAccumulator {
        private final String knowledgePoint;
        private int wrongCount = 0;
        private final List<String> sources = new ArrayList<>();
        private final List<Long> questionIds = new ArrayList<>();

        private WeaknessAccumulator(String knowledgePoint) {
            this.knowledgePoint = knowledgePoint;
        }

        private void addSource(String source, int count) {
            wrongCount += count;
            if (source != null && !source.isBlank() && !sources.contains(source)) {
                sources.add(source);
            }
        }

        private void addQuestion(Long questionId) {
            if (questionId != null && !questionIds.contains(questionId)) {
                questionIds.add(questionId);
            }
        }
    }
}
