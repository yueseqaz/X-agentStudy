package com.xagentstudy.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.GeneratedAnswer;
import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.profile.LearningProfileRepository;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QAServiceTest {
    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsPlanContextToAiWhenNoDocumentsHaveBeenUploaded() {
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
        LearningProfileRepository profileRepository = mock(LearningProfileRepository.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        KnowledgeDocumentRepository documentRepository = mock(KnowledgeDocumentRepository.class);
        AgentGenerationService agentGenerationService = mock(AgentGenerationService.class);
        QARecordRepository qaRecordRepository = mock(QARecordRepository.class);
        QAService service = new QAService(
                planRepository,
                directionRepository,
                profileRepository,
                chunkService,
                documentRepository,
                agentGenerationService,
                qaRecordRepository,
                new ObjectMapper()
        );

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(5L, "user@example.com", "USER"),
                null
        ));
        LearningPlan plan = new LearningPlan(
                7L,
                null,
                "Redis 系统学习计划",
                "ACTIVE",
                "掌握 Redis 缓存、持久化和一致性问题",
                """
                        [{"name":"缓存设计","focus":"理解缓存命中、过期和淘汰","knowledgePoints":[{"title":"缓存穿透"},{"title":"缓存雪崩"}]}]
                        """
        );
        ReflectionTestUtils.setField(plan, "id", 11L);
        LearningDirection direction = new LearningDirection(5L, "Redis", "后端", "缓存中间件");
        ReflectionTestUtils.setField(direction, "id", 7L);

        when(planRepository.findById(11L)).thenReturn(Optional.of(plan));
        when(directionRepository.findById(7L)).thenReturn(Optional.of(direction));
        when(chunkService.retrieve(11L, "冷启动时缓存穿透怎么处理？", 4)).thenReturn(List.of());
        when(documentRepository.findAllById(List.of())).thenReturn(List.of());
        when(agentGenerationService.generateAnswer(eq("冷启动时缓存穿透怎么处理？"), eq("FOUNDATION"), any(), eq("")))
                .thenReturn(new GeneratedAnswer("围绕 Redis 回答", "[\"缓存穿透\"]", "[]"));
        when(qaRecordRepository.save(any(QARecord.class))).thenAnswer(invocation -> {
            QARecord record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 91L);
            return record;
        });

        QAResponse response = service.ask(11L, new AskQuestionRequest("冷启动时缓存穿透怎么处理？"));

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentGenerationService).generateAnswer(eq("冷启动时缓存穿透怎么处理？"), eq("FOUNDATION"), contextCaptor.capture(), eq(""));
        assertThat(contextCaptor.getValue()).contains("Redis 系统学习计划", "缓存穿透", "缓存雪崩");
        assertThat(response.answer()).isEqualTo("围绕 Redis 回答");
    }
}
