package com.xagentstudy.report;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.checkin.DailyCheckinRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeeklyReviewService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final LearningReportService reportService;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final DailyCheckinRepository checkinRepository;

    public WeeklyReviewService(
            LearningReportService reportService,
            PlanTaskRecordRepository taskRecordRepository,
            ReviewRecordRepository reviewRecordRepository,
            DailyCheckinRepository checkinRepository
    ) {
        this.reportService = reportService;
        this.taskRecordRepository = taskRecordRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.checkinRepository = checkinRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyReviewResponse getWeeklyReview(Long planId) {
        Long userId = AuthContext.currentUserId();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        OffsetDateTime start = weekStart.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime end = weekEnd.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();

        LearningReportResponse report = reportService.getReport(planId);
        int completedTaskCount = toInt(taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrueAndCompletedAtBetween(planId, userId, start, end));
        List<ReviewRecord> reviews = reviewRecordRepository.findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(planId, userId);
        int completedReviewCount = (int) reviews.stream()
                .filter(record -> Boolean.TRUE.equals(record.getCompleted()))
                .filter(record -> record.getCompletedAt() != null && !record.getCompletedAt().isBefore(start) && record.getCompletedAt().isBefore(end))
                .count();
        int checkinDays = checkinRepository.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, weekStart, weekEnd).size();

        return new WeeklyReviewResponse(
                planId,
                weekStart,
                weekEnd,
                completedTaskCount,
                report.answeredQuestionCount(),
                report.correctQuestionCount(),
                report.wrongQuestionCount(),
                report.accuracyRate(),
                completedReviewCount,
                checkinDays,
                report.masteryScore(),
                highlights(completedTaskCount, report, completedReviewCount, checkinDays),
                risks(report, reviews),
                nextWeekFocus(report),
                report.trend()
        );
    }

    private List<String> highlights(int completedTaskCount, LearningReportResponse report, int completedReviewCount, int checkinDays) {
        List<String> highlights = new ArrayList<>();
        highlights.add("本周完成 " + completedTaskCount + " 个计划任务");
        highlights.add("完成 " + report.answeredQuestionCount() + " 道题，正确率 " + report.accuracyRate() + "%");
        highlights.add("完成 " + completedReviewCount + " 个复习项，打卡 " + checkinDays + " 天");
        return highlights;
    }

    private List<String> risks(LearningReportResponse report, List<ReviewRecord> reviews) {
        List<String> risks = new ArrayList<>();
        long openReviewCount = reviews.stream().filter(record -> !Boolean.TRUE.equals(record.getCompleted())).count();
        if (report.wrongQuestionCount() > 0) {
            risks.add("仍有 " + report.wrongQuestionCount() + " 道错题需要回炉。");
        }
        if (openReviewCount > 0) {
            risks.add("还有 " + openReviewCount + " 个复习项未完成。");
        }
        if (report.accuracyRate() < 70 && report.answeredQuestionCount() > 0) {
            risks.add("正确率低于 70%，下周先降难度修复基础。");
        }
        if (risks.isEmpty()) {
            risks.add("本周节奏稳定，可以追加更高难度练习。");
        }
        return risks;
    }

    private List<String> nextWeekFocus(LearningReportResponse report) {
        List<String> focus = new ArrayList<>();
        if (!report.weakPoints().isEmpty()) {
            focus.add("优先修复：" + String.join("、", report.weakPoints().subList(0, Math.min(3, report.weakPoints().size()))));
        }
        focus.addAll(report.nextActions().stream().limit(3).toList());
        return focus;
    }

    private int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
