package com.xagentstudy.report;

import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.profile.LearningProfile;
import com.xagentstudy.profile.LearningProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicProfileServiceTest {
    @Test
    void buildsDynamicProfileWithSixDimensionsAndUpdateSignals() {
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        LearningProfileRepository profileRepository = mock(LearningProfileRepository.class);
        KnowledgeDocumentRepository documentRepository = mock(KnowledgeDocumentRepository.class);
        LearningReportService reportService = mock(LearningReportService.class);
        DynamicProfileService service = new DynamicProfileService(
                planRepository,
                profileRepository,
                documentRepository,
                reportService
        );
        LearningPlan plan = new LearningPlan(
                12L,
                22L,
                "Redis 系统学习计划",
                "ACTIVE",
                "掌握 Redis 缓存和持久化能力",
                "[]"
        );
        LearningProfile profile = new LearningProfile(
                12L,
                "掌握 Redis 实战",
                "有 Java 基础，Redis 入门",
                "每天 1 小时",
                "喜欢案例和图解",
                "[\"持久化容易混淆\"]",
                "先补基础，再做项目",
                "[]"
        );
        KnowledgeDocument document = new KnowledgeDocument(16L, "Redis 持久化讲义", "MARKDOWN", "uploads/redis.md");
        document.markParseSuccess("讲解 RDB 和 AOF");
        LearningReportResponse report = new LearningReportResponse(
                16L,
                8,
                6,
                6,
                4,
                2,
                66.7,
                50.0,
                58,
                List.of("AOF 重写", "RDB 快照"),
                List.of("AOF 重写 出现 2 次错题，建议先复述概念再重做相关题。"),
                List.of(),
                List.of(),
                List.of("优先复习：AOF 重写、RDB 快照。"),
                List.of("生成 3 道同知识点低难度题，先稳定正确率。"),
                List.of()
        );

        when(planRepository.findById(16L)).thenReturn(Optional.of(plan));
        when(profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(12L)).thenReturn(Optional.of(profile));
        when(documentRepository.findByPlanIdOrderByUploadedAtDesc(16L)).thenReturn(List.of(document));
        when(reportService.getReport(16L)).thenReturn(report);

        DynamicProfileResponse response = service.getDynamicProfile(16L);

        assertThat(response.completenessScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.dimensions()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(response.dimensions()).extracting(DynamicProfileDimensionResponse::name)
                .contains("知识基础", "学习目标", "学习风格", "薄弱点", "学习节奏", "资源偏好");
        assertThat(response.updateSignals()).anyMatch(signal -> signal.reason().contains("AOF 重写"));
        assertThat(response.recommendationReasons()).anyMatch(reason -> reason.reason().contains("薄弱点"));
    }
}
