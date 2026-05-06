package com.xagentstudy.practice;

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

class PracticeProjectServiceTest {
    private final LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
    private final LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
    private final LearningReportService reportService = mock(LearningReportService.class);
    private final PracticeProjectService service = new PracticeProjectService(
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
    void buildsPracticeProjectFromCurrentStage() throws Exception {
        mockCurrentUser(7L);
        when(planRepository.findById(16L)).thenReturn(Optional.of(plan()));
        when(directionRepository.findById(3L)).thenReturn(Optional.of(new LearningDirection(7L, "Redis", "后端", "掌握 Redis")));
        when(reportService.getReport(16L)).thenReturn(report());

        PracticeProjectResponse response = service.getProject(16L);

        assertThat(response.planId()).isEqualTo(16L);
        assertThat(response.title()).contains("Redis 基础");
        assertThat(response.tasks()).hasSize(4);
        assertThat(response.tasks().get(0).knowledgePoints()).contains("Key 命名");
        assertThat(response.tasks().get(0).title()).contains("Redis 基础", "Key 命名");
        assertThat(response.tasks().get(1).description()).contains("过期时间", "核心功能方案");
        assertThat(response.tasks().get(2).title()).contains("过期时间");
        assertThat(response.deliverables()).contains("项目说明");
    }

    @Test
    void evaluatesSubmittedPracticeProject() throws Exception {
        mockCurrentUser(7L);
        when(planRepository.findById(16L)).thenReturn(Optional.of(plan()));
        when(directionRepository.findById(3L)).thenReturn(Optional.of(new LearningDirection(7L, "Redis", "后端", "掌握 Redis")));
        when(reportService.getReport(16L)).thenReturn(report());

        PracticeProjectResultResponse response = service.submit(16L, new SubmitPracticeProjectRequest(
                "我完成了 Redis 用户资料缓存小项目，使用 user:profile:1001 命名，并设置过期时间控制数据生命周期。",
                "通过 TTL 策略、缓存回源和异常兜底处理热点数据，说明了为什么这样设计。",
                Map.of(
                        "task-1", "设计 Key 命名规范并说明原因。",
                        "task-2", "完成 String 和 Hash 的缓存结构设计。",
                        "task-3", "补充过期时间策略和风险说明。",
                        "task-4", "整理项目说明和复盘。"
                )
        ));

        assertThat(response.passed()).isTrue();
        assertThat(response.totalScore()).isGreaterThanOrEqualTo(70);
        assertThat(response.outcomeCardTitle()).isEqualTo("Redis 基础 实战成果卡");
        assertThat(response.coveredKnowledgePoints()).contains("Key 命名", "过期时间");
        assertThat(response.taskResults()).hasSize(4);
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
