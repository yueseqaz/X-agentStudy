package com.xagentstudy.report;

import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.checkin.DailyCheckin;
import com.xagentstudy.checkin.DailyCheckinRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeeklyReviewServiceTest {
    private final LearningReportService reportService = mock(LearningReportService.class);
    private final PlanTaskRecordRepository taskRecordRepository = mock(PlanTaskRecordRepository.class);
    private final ReviewRecordRepository reviewRecordRepository = mock(ReviewRecordRepository.class);
    private final DailyCheckinRepository checkinRepository = mock(DailyCheckinRepository.class);
    private final WeeklyReviewService service = new WeeklyReviewService(
            reportService,
            taskRecordRepository,
            reviewRecordRepository,
            checkinRepository
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsWeeklyReviewFromPlanReportAndCheckins() {
        mockCurrentUser(7L);
        LearningReportResponse report = new LearningReportResponse(
                16L,
                10,
                7,
                6,
                4,
                2,
                66.7,
                40.0,
                55,
                List.of("事务隔离", "索引选择"),
                List.of("事务隔离 出现 1 次错题，建议先复述概念再重做相关题。"),
                List.of(),
                List.of(),
                List.of("优先复习：事务隔离、索引选择。"),
                List.of("进入复习页同步薄弱点，并按间隔复习完成今日清单。"),
                List.of(new StudyTrendPoint("2026-05-04", 4, 2), new StudyTrendPoint("2026-05-05", 3, 2))
        );
        ReviewRecord completed = new ReviewRecord(16L, 7L, "事务隔离", "来自错题聚合", 2);
        completed.complete(4);
        when(reportService.getReport(16L)).thenReturn(report);
        when(taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrueAndCompletedAtBetween(org.mockito.ArgumentMatchers.eq(16L), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(3L);
        when(reviewRecordRepository.findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(16L, 7L))
                .thenReturn(List.of(completed));
        when(checkinRepository.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDate.class), org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(new DailyCheckin(7L, LocalDate.of(2026, 5, 5))));

        WeeklyReviewResponse response = service.getWeeklyReview(16L);

        assertThat(response.planId()).isEqualTo(16L);
        assertThat(response.completedTaskCount()).isEqualTo(3);
        assertThat(response.checkinDays()).isEqualTo(1);
        assertThat(response.completedReviewCount()).isEqualTo(1);
        assertThat(response.highlights()).contains("本周完成 3 个计划任务");
        assertThat(response.nextWeekFocus()).contains("优先修复：事务隔离、索引选择");
    }

    private void mockCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "demo", "USER"),
                null,
                List.of()
        ));
    }
}
