package com.xagentstudy.dashboard;

import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.review.ReviewRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningCoachServiceTest {
    @Mock
    private LearningDirectionRepository directionRepository;
    @Mock
    private LearningPlanRepository planRepository;
    @Mock
    private KnowledgeDocumentRepository documentRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private PlanTaskRecordRepository taskRecordRepository;

    @InjectMocks
    private LearningCoachService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendsCreatingDirectionWhenUserHasNoPlan() {
        mockCurrentUser(7L);
        when(directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(7L)).thenReturn(List.of());

        LearningCoachResponse response = service.today();

        assertThat(response.headline()).isEqualTo("先定一个清晰学习方向");
        assertThat(response.primaryActionPath()).isEqualTo("/directions");
        assertThat(response.estimatedMinutes()).isEqualTo(12);
        assertThat(response.nextSteps()).contains("创建一个具体学习方向");
    }

    @Test
    void returnsFallbackAdviceWhenCoachDataCannotBeLoaded() {
        mockCurrentUser(7L);
        when(directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(7L))
                .thenThrow(new IllegalStateException("database unavailable"));

        LearningCoachResponse response = service.today();

        assertThat(response.headline()).isEqualTo("先定一个清晰学习方向");
        assertThat(response.primaryActionPath()).isEqualTo("/directions");
    }

    @Test
    void recommendsUploadingKnowledgeWhenLatestPlanHasNoDocumentsOrQuestions() throws Exception {
        mockCurrentUser(7L);
        LearningDirection direction = new LearningDirection(7L, "Vue 前端", "前端开发", "目标");
        setField(direction, "id", 21L);
        LearningPlan plan = new LearningPlan(21L, 31L, "Vue 前端 30 天计划", "ACTIVE", "掌握 Vue", "[]");
        setField(plan, "id", 41L);

        when(directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(7L)).thenReturn(List.of(direction));
        when(planRepository.findByDirectionIdInOrderByCreatedAtDesc(List.of(21L))).thenReturn(List.of(plan));
        when(documentRepository.countByPlanIdIn(List.of(41L))).thenReturn(0L);
        when(questionRepository.countByPlanIdIn(List.of(41L))).thenReturn(0L);
        when(reviewRecordRepository.countByPlanIdInAndUserIdAndCompletedFalse(List.of(41L), 7L)).thenReturn(0L);
        when(taskRecordRepository.countByPlanIdAndUserId(41L, 7L)).thenReturn(6L);
        when(taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrue(41L, 7L)).thenReturn(1L);

        LearningCoachResponse response = service.today();

        assertThat(response.planId()).isEqualTo(41L);
        assertThat(response.directionName()).isEqualTo("Vue 前端");
        assertThat(response.headline()).isEqualTo("先给计划补一份核心资料");
        assertThat(response.primaryActionPath()).isEqualTo("/plans/41/knowledge");
        assertThat(response.reason()).contains("还没有知识库资料");
        assertThat(response.signals()).contains("任务完成 1/6");
    }

    private void mockCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "demo", "USER"),
                null,
                List.of()
        ));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
