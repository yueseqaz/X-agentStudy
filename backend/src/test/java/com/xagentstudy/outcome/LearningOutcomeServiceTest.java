package com.xagentstudy.outcome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.checkin.DailyCheckin;
import com.xagentstudy.checkin.DailyCheckinRepository;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningOutcomeServiceTest {
    @Mock
    private DailyCheckinRepository dailyCheckinRepository;
    @Mock
    private LearningDirectionRepository directionRepository;
    @Mock
    private LearningPlanRepository planRepository;
    @Mock
    private PlanTaskRecordRepository taskRecordRepository;
    @Mock
    private AnswerRecordRepository answerRecordRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private SummaryCardRepository summaryCardRepository;
    @Mock
    private LearningReportService learningReportService;

    private LearningOutcomeService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aggregatesOutcomeSnapshotForCurrentUser() throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-30T08:00:00Z"), ZoneOffset.UTC);
        service = new LearningOutcomeService(
                dailyCheckinRepository,
                directionRepository,
                planRepository,
                taskRecordRepository,
                answerRecordRepository,
                questionRepository,
                reviewRecordRepository,
                summaryCardRepository,
                learningReportService,
                new ObjectMapper(),
                fixedClock
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(1L, "demo", "USER"),
                null,
                List.of()
        ));

        LearningDirection direction = new LearningDirection(1L, "Java 后端", "开发", "desc");
        setField(direction, "id", 11L);

        LearningPlan plan = new LearningPlan(11L, null, "Spring Boot 冲刺", "ACTIVE", "goal", "[]");
        setField(plan, "id", 21L);

        DailyCheckin checkin1 = new DailyCheckin(1L, LocalDate.of(2026, 4, 2));
        DailyCheckin checkin2 = new DailyCheckin(1L, LocalDate.of(2026, 4, 20));

        PlanTaskRecord task1 = completedTask(21L, 1L, 0, 0, "读文档", OffsetDateTime.parse("2026-04-03T10:00:00Z"));
        PlanTaskRecord task2 = completedTask(21L, 1L, 0, 1, "做练习", OffsetDateTime.parse("2026-04-05T10:00:00Z"));

        Question question1 = new Question(21L, "PLAN", "SINGLE_CHOICE", "EASY", "Q1", "[]", "\"A\"", "exp", "[\"IOC\"]");
        Question question2 = new Question(21L, "PLAN", "SINGLE_CHOICE", "EASY", "Q2", "[]", "\"B\"", "exp", "[\"AOP\"]");
        setField(question1, "id", 31L);
        setField(question2, "id", 32L);

        AnswerRecord correctAnswer = new AnswerRecord(31L, 1L, "\"A\"", true);
        AnswerRecord wrongAnswer = new AnswerRecord(32L, 1L, "\"C\"", false);
        setField(correctAnswer, "answeredAt", OffsetDateTime.parse("2026-04-06T10:00:00Z"));
        setField(wrongAnswer, "answeredAt", OffsetDateTime.parse("2026-04-07T10:00:00Z"));

        ReviewRecord reviewRecord = new ReviewRecord(21L, 1L, "IOC", "reason", 2);
        reviewRecord.complete(4);
        setField(reviewRecord, "recommendedAt", OffsetDateTime.parse("2026-04-08T10:00:00Z"));
        setField(reviewRecord, "completedAt", OffsetDateTime.parse("2026-04-08T10:00:00Z"));

        SummaryCard summaryCard = new SummaryCard(1L, 21L, null, "IOC 总结", "掌握依赖注入主线", "spring.pdf", "{}", "DEFAULT");
        setField(summaryCard, "id", 41L);
        setField(summaryCard, "createdAt", OffsetDateTime.parse("2026-04-09T10:00:00Z"));

        LearningReportResponse report = new LearningReportResponse(
                21L,
                2,
                2,
                2,
                1,
                1,
                50.0,
                100.0,
                78,
                List.of("AOP"),
                List.of("AOP 错题较多"),
                List.of(),
                List.of(),
                List.of("优先复习 AOP"),
                List.of("继续完成今日复习"),
                List.of()
        );

        when(directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(1L)).thenReturn(List.of(direction));
        when(planRepository.findByDirectionIdInOrderByCreatedAtDesc(List.of(11L))).thenReturn(List.of(plan));
        when(dailyCheckinRepository.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(
                1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(checkin1, checkin2));
        when(taskRecordRepository.countByUserIdAndCompletedTrueAndCompletedAtBetween(eq(1L), any(), any())).thenReturn(2L);
        when(taskRecordRepository.findByUserIdAndPlanIdInOrderByPlanIdAscStageIndexAscTaskIndexAsc(1L, List.of(21L)))
                .thenReturn(List.of(task1, task2));
        when(answerRecordRepository.findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(correctAnswer, wrongAnswer));
        when(answerRecordRepository.findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(List.of(31L, 32L), 1L))
                .thenReturn(List.of(wrongAnswer, correctAnswer));
        when(questionRepository.findAllById(List.of(31L))).thenReturn(List.of(question1));
        when(questionRepository.findByPlanIdOrderByCreatedAtDesc(21L)).thenReturn(List.of(question1, question2));
        when(reviewRecordRepository.findByUserIdAndPlanIdInOrderByRecommendedAtDesc(1L, List.of(21L)))
                .thenReturn(List.of(reviewRecord));
        when(summaryCardRepository.findTop5ByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(1L), any(), any()))
                .thenReturn(List.of(summaryCard));
        when(learningReportService.getReport(21L)).thenReturn(report);

        LearningOutcomeResponse response = service.getOutcomes(null, null, null);

        assertThat(response.rangeCode()).isEqualTo("LAST_30_DAYS");
        assertThat(response.overview().learningDays()).isEqualTo(2);
        assertThat(response.overview().completedPlanCount()).isEqualTo(1);
        assertThat(response.overview().masteredKnowledgePointCount()).isEqualTo(1);
        assertThat(response.directions()).hasSize(1);
        assertThat(response.directions().get(0).directionName()).isEqualTo("Java 后端");
        assertThat(response.directions().get(0).strengths()).containsExactly("IOC");
        assertThat(response.directions().get(0).weakPoints()).containsExactly("AOP");
        assertThat(response.trends()).hasSize(30);
        assertThat(response.trends().stream()
                .filter(item -> item.date().equals(LocalDate.of(2026, 4, 6)))
                .findFirst()
                .orElseThrow()
                .quizAttempts()).isEqualTo(1);
        assertThat(response.trends().stream()
                .filter(item -> item.date().equals(LocalDate.of(2026, 4, 6)))
                .findFirst()
                .orElseThrow()
                .correctAnswers()).isEqualTo(1);
        assertThat(response.highlights()).extracting(item -> item.title()).contains("IOC 总结");
        assertThat(response.suggestions()).isNotEmpty();
        assertThat(response.poster().keywords()).contains("Java 后端", "IOC");
    }

    private static PlanTaskRecord completedTask(
            Long planId,
            Long userId,
            int stageIndex,
            int taskIndex,
            String taskText,
            OffsetDateTime completedAt
    ) throws Exception {
        PlanTaskRecord record = new PlanTaskRecord(planId, userId, stageIndex, taskIndex, taskText);
        setField(record, "completed", true);
        setField(record, "completedAt", completedAt);
        return record;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
