package com.xagentstudy.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.report.LearningReportResponse;
import com.xagentstudy.report.LearningReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StageAssessmentServiceTest {
    private final LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
    private final LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
    private final LearningReportService reportService = mock(LearningReportService.class);
    private final StageAssessmentService service = new StageAssessmentService(
            planRepository,
            directionRepository,
            reportService,
            new ObjectMapper()
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsAssessmentForCurrentStage() throws Exception {
        mockCurrentUser(7L);
        LearningPlan plan = plan();
        when(planRepository.findById(16L)).thenReturn(Optional.of(plan));
        when(directionRepository.findById(3L)).thenReturn(Optional.of(new LearningDirection(7L, "Redis", "后端", "掌握 Redis")));
        when(reportService.getReport(16L)).thenReturn(report());

        StageAssessmentResponse response = service.getAssessment(16L);

        assertThat(response.planId()).isEqualTo(16L);
        assertThat(response.stageIndex()).isEqualTo(0);
        assertThat(response.stageName()).isEqualTo("Redis 基础");
        assertThat(response.challenges()).hasSize(3);
        assertThat(response.readinessScore()).isEqualTo(55);
        assertThat(response.challenges().get(0).rubric()).contains("Key 命名");
        assertThat(response.challenges().get(0).prompt()).contains("Redis 基础", "Key 命名", "过期时间");
        assertThat(response.challenges().get(1).prompt()).contains("Redis 基础", "掌握 Key 命名、数据结构和持久化基础");
        assertThat(response.challenges().get(2).prompt()).contains("过期时间");
    }

    @Test
    void gradesSubmissionAndReturnsPassCard() throws Exception {
        mockCurrentUser(7L);
        LearningPlan plan = plan();
        when(planRepository.findById(16L)).thenReturn(Optional.of(plan));
        when(directionRepository.findById(3L)).thenReturn(Optional.of(new LearningDirection(7L, "Redis", "后端", "掌握 Redis")));
        when(reportService.getReport(16L)).thenReturn(report());

        StageAssessmentResultResponse response = service.submit(16L, new SubmitStageAssessmentRequest(Map.of(
                "concept-check", "Key 命名需要包含业务前缀、实体、标识，并设置过期时间来控制数据生命周期。",
                "case-transfer", "我会使用 user:profile:1001 这类命名，热点数据单独设置 TTL，避免长期占用内存。",
                "interview-defense", "RDB 适合快照恢复，AOF 更细粒度，两者要结合恢复速度和数据丢失风险来选择。"
        )));

        assertThat(response.passed()).isTrue();
        assertThat(response.totalScore()).isGreaterThanOrEqualTo(70);
        assertThat(response.level()).isEqualTo("掌握稳定");
        assertThat(response.passCardTitle()).isEqualTo("Redis 基础 阶段通过卡片");
        assertThat(response.results()).hasSize(3);
    }

    private LearningPlan plan() throws Exception {
        String stages = """
                [
                  {
                    "name": "Redis 基础",
                    "focus": "掌握 Key 命名、数据结构和持久化基础",
                    "duration": "1 周",
                    "tasks": ["完成 Key 命名学习", "完成持久化练习"],
                    "units": [
                      {
                        "name": "Key 设计",
                        "goal": "能设计可维护的 Redis Key",
                        "knowledgePoints": [
                          {"id":"kp-1","title":"Key 命名","level":"基础","outcome":"能设计规范 Key","estimatedMinutes":30},
                          {"id":"kp-2","title":"过期时间","level":"基础","outcome":"能解释 TTL 策略","estimatedMinutes":30}
                        ]
                      }
                    ]
                  }
                ]
                """;
        LearningPlan plan = new LearningPlan(3L, 2L, "Redis 系统学习计划", "ACTIVE", "掌握 Redis", stages);
        setField(plan, "id", 16L);
        return plan;
    }

    private LearningReportResponse report() {
        return new LearningReportResponse(
                16L,
                6,
                4,
                4,
                3,
                1,
                75.0,
                40.0,
                55,
                List.of("过期时间"),
                List.of("过期时间 出现 1 次错题。"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
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
