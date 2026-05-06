package com.xagentstudy.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.resource.LearningResourceResponse;
import com.xagentstudy.resource.LearningResourceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceQualityServiceTest {
    @Test
    void scoresResourceQualityAcrossCoverageAndLearningLoop() {
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        KnowledgeDocumentRepository documentRepository = mock(KnowledgeDocumentRepository.class);
        LearningReportService reportService = mock(LearningReportService.class);
        LearningResourceService resourceService = mock(LearningResourceService.class);
        ResourceQualityService service = new ResourceQualityService(
                planRepository,
                documentRepository,
                reportService,
                resourceService,
                new ObjectMapper()
        );
        LearningPlan plan = new LearningPlan(
                2L,
                8L,
                "Redis 系统学习计划",
                "ACTIVE",
                "掌握 Redis",
                """
                [{"name":"基础","units":[{"name":"数据结构","knowledgePoints":[{"title":"String"},{"title":"Hash"}]}]}]
                """
        );
        KnowledgeDocument document = new KnowledgeDocument(16L, "String 讲义", "MARKDOWN", "uploads/string.md");
        document.markParseSuccess("讲解 Redis String");
        LearningReportResponse report = new LearningReportResponse(
                16L,
                6,
                4,
                4,
                3,
                1,
                75,
                50,
                68,
                List.of("Hash 冲突"),
                List.of("Hash 冲突 出现 1 次错题。"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        when(planRepository.findById(16L)).thenReturn(Optional.of(plan));
        when(documentRepository.findByPlanIdOrderByUploadedAtDesc(16L)).thenReturn(List.of(document));
        when(reportService.getReport(16L)).thenReturn(report);
        when(resourceService.recommended(16L)).thenReturn(List.of(
                new LearningResourceResponse(1L, "Redis 视频课", "", "EXTERNAL_COURSE", "https://bilibili.com/video/BV1", "text/html", "Redis", "后端", "Redis", List.of("Redis"), "https://bilibili.com/video/BV1", 0L, null)
        ));

        ResourceQualityResponse response = service.getQuality(16L);

        assertThat(response.overallScore()).isGreaterThan(50);
        assertThat(response.items()).extracting(ResourceQualityItemResponse::name)
                .contains("知识覆盖", "多模态完整度", "测验闭环", "个性化匹配");
        assertThat(response.suggestions()).isNotEmpty();
    }
}
