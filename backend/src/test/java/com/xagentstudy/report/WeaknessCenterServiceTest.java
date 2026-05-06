package com.xagentstudy.report;

import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeaknessCenterServiceTest {
    private final LearningReportService reportService = mock(LearningReportService.class);
    private final ReviewRecordRepository reviewRecordRepository = mock(ReviewRecordRepository.class);
    private final WeaknessCenterService service = new WeaknessCenterService(reportService, reviewRecordRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsWeaknessCenterFromReportSourcesAndReviewRecords() {
        mockCurrentUser(7L);
        LearningReportResponse report = new LearningReportResponse(
                16L,
                8,
                5,
                5,
                3,
                2,
                60.0,
                25.0,
                48,
                List.of("Redis 持久化"),
                List.of("Redis 持久化 出现 2 次错题，建议先复述概念再重做相关题。"),
                List.of(new WeakPointSourceResponse("Redis 持久化", "DOCUMENT:2", "资料 #2", 2)),
                List.of(new WrongQuestionResponse(99L, "AOF 与 RDB 的区别是什么？", "AOF", "AOF + RDB", "需要区分恢复速度和日志粒度。", List.of("Redis 持久化"), "DOCUMENT:2", "资料 #2", null)),
                List.of("优先复习：Redis 持久化。"),
                List.of("生成 3 道同知识点低难度题，先稳定正确率。"),
                List.of()
        );
        ReviewRecord review = new ReviewRecord(16L, 7L, "Redis 持久化", "来自错题聚合", 3);
        when(reportService.getReport(16L)).thenReturn(report);
        when(reviewRecordRepository.findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(16L, 7L))
                .thenReturn(List.of(review));

        WeaknessCenterResponse response = service.getCenter(16L);

        assertThat(response.planId()).isEqualTo(16L);
        assertThat(response.summary().openWeaknessCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).knowledgePoint()).isEqualTo("Redis 持久化");
        assertThat(response.items().get(0).status()).isEqualTo("修复中");
        assertThat(response.items().get(0).wrongCount()).isEqualTo(2);
        assertThat(response.items().get(0).actions()).contains("去复习", "生成针对练习");
    }

    private void mockCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "demo", "USER"),
                null,
                List.of()
        ));
    }
}
