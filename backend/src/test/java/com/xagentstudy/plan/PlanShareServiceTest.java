package com.xagentstudy.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.task.AgentTaskRepository;
import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentService;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.profile.LearningProfileRepository;
import com.xagentstudy.qa.QARecordRepository;
import com.xagentstudy.quiz.AnswerRecordRepository;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.report.LearningReportService;
import com.xagentstudy.review.ReviewRecordRepository;
import com.xagentstudy.summary.SummaryCardService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanShareServiceTest {
    @Test
    void enablesShareCodeAndReadsPublicPlan() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthPrincipal(5L, "user@example.com", "USER"), null)
        );
        LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
        LearningProfileRepository profileRepository = mock(LearningProfileRepository.class);
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        LearningPlanService service = new LearningPlanService(
                directionRepository,
                profileRepository,
                planRepository,
                mock(AgentGenerationService.class),
                mock(PlanTaskRecordRepository.class),
                mock(LearningReportService.class),
                mock(KnowledgeDocumentRepository.class),
                mock(KnowledgeDocumentService.class),
                mock(QARecordRepository.class),
                mock(QuestionRepository.class),
                mock(AnswerRecordRepository.class),
                mock(ReviewRecordRepository.class),
                mock(AgentTaskRepository.class),
                mock(SummaryCardService.class),
                mock(BillingService.class),
                new ObjectMapper()
        );
        LearningDirection direction = new LearningDirection(5L, "机器学习", "AI", "课程");
        ReflectionTestUtils.setField(direction, "id", 7L);
        LearningPlan plan = new LearningPlan(7L, 3L, "机器学习入门计划", "ACTIVE", "掌握基础模型", "[]");
        ReflectionTestUtils.setField(plan, "id", 11L);

        when(planRepository.findById(11L)).thenReturn(Optional.of(plan));
        when(directionRepository.findById(7L)).thenReturn(Optional.of(direction));
        when(planRepository.findByShareCode("share-token")).thenReturn(Optional.of(plan));

        PlanShareResponse share = service.enableShare(11L);
        ReflectionTestUtils.setField(plan, "shareCode", "share-token");
        PublicPlanResponse publicPlan = service.getPublicPlan("share-token");

        assertThat(share.shareCode()).isNotBlank();
        assertThat(publicPlan.title()).isEqualTo("机器学习入门计划");
        assertThat(publicPlan.directionName()).isEqualTo("机器学习");
        assertThat(publicPlan.goal()).isEqualTo("掌握基础模型");
    }

    @Test
    void appliesSharedPlanToCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthPrincipal(12L, "other@example.com", "USER"), null)
        );
        LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
        LearningProfileRepository profileRepository = mock(LearningProfileRepository.class);
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        BillingService billingService = mock(BillingService.class);
        LearningPlanService service = new LearningPlanService(
                directionRepository,
                profileRepository,
                planRepository,
                mock(AgentGenerationService.class),
                mock(PlanTaskRecordRepository.class),
                mock(LearningReportService.class),
                mock(KnowledgeDocumentRepository.class),
                mock(KnowledgeDocumentService.class),
                mock(QARecordRepository.class),
                mock(QuestionRepository.class),
                mock(AnswerRecordRepository.class),
                mock(ReviewRecordRepository.class),
                mock(AgentTaskRepository.class),
                mock(SummaryCardService.class),
                billingService,
                new ObjectMapper()
        );
        LearningDirection sourceDirection = new LearningDirection(5L, "机器学习", "AI", "课程");
        ReflectionTestUtils.setField(sourceDirection, "id", 7L);
        LearningPlan sourcePlan = new LearningPlan(7L, 3L, "机器学习入门计划", "ACTIVE", "掌握基础模型", "[]");
        ReflectionTestUtils.setField(sourcePlan, "id", 11L);
        ReflectionTestUtils.setField(sourcePlan, "shareCode", "share-token");

        when(planRepository.findByShareCode("share-token")).thenReturn(Optional.of(sourcePlan));
        when(directionRepository.findById(7L)).thenReturn(Optional.of(sourceDirection));
        when(directionRepository.save(any(LearningDirection.class))).thenAnswer(invocation -> {
            LearningDirection direction = invocation.getArgument(0);
            ReflectionTestUtils.setField(direction, "id", 20L);
            return direction;
        });
        when(planRepository.save(any(LearningPlan.class))).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            ReflectionTestUtils.setField(plan, "id", 30L);
            return plan;
        });

        PlanResponse applied = service.applySharedPlan("share-token");

        assertThat(applied.id()).isEqualTo(30L);
        assertThat(applied.directionId()).isEqualTo(20L);
        assertThat(applied.title()).isEqualTo("机器学习入门计划");
        assertThat(applied.directionName()).isEqualTo("机器学习");
    }
}
