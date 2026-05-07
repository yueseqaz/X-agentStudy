package com.xagentstudy.community;

import com.xagentstudy.agent.model.ModelGateway;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityServiceTest {
    @Test
    void publishesQuestionAndUserAnswer() {
        CommunityQuestionRepository questionRepository = mock(CommunityQuestionRepository.class);
        CommunityAnswerRepository answerRepository = mock(CommunityAnswerRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        ModelGateway modelGateway = mock(ModelGateway.class);
        CommunityService service = new CommunityService(
                questionRepository,
                answerRepository,
                userRepository,
                modelGateway,
                mock(LearningPlanRepository.class),
                mock(LearningDirectionRepository.class)
        );

        AppUser user = user(9L, "小林");
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(questionRepository.save(any(CommunityQuestion.class))).thenAnswer(invocation -> {
            CommunityQuestion question = invocation.getArgument(0);
            ReflectionTestUtils.setField(question, "id", 21L);
            return question;
        });
        when(questionRepository.findById(21L)).thenReturn(Optional.of(question(21L, 9L)));
        when(answerRepository.save(any(CommunityAnswer.class))).thenAnswer(invocation -> {
            CommunityAnswer answer = invocation.getArgument(0);
            ReflectionTestUtils.setField(answer, "id", 33L);
            return answer;
        });

        CommunityQuestionResponse created = service.createQuestion(9L, new CreateCommunityQuestionRequest(
                null,
                "Redis AOF 为什么会重写？",
                "学习持久化时不太理解 AOF 重写触发条件。",
                "Redis,持久化"
        ));
        CommunityAnswerResponse answer = service.createAnswer(9L, 21L, new CreateCommunityAnswerRequest(
                "AOF 重写是为了压缩历史命令，减少文件体积。"
        ));

        assertThat(created.id()).isEqualTo(21L);
        assertThat(created.authorName()).isEqualTo("小林");
        assertThat(answer.source()).isEqualTo("USER");
        assertThat(answer.content()).contains("压缩历史命令");
        verify(answerRepository).save(any(CommunityAnswer.class));
    }

    @Test
    void createsFallbackAiAnswerWhenContentMentionsAiAndModelIsUnavailable() {
        CommunityQuestionRepository questionRepository = mock(CommunityQuestionRepository.class);
        CommunityAnswerRepository answerRepository = mock(CommunityAnswerRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        ModelGateway modelGateway = mock(ModelGateway.class);
        CommunityService service = new CommunityService(
                questionRepository,
                answerRepository,
                userRepository,
                modelGateway,
                mock(LearningPlanRepository.class),
                mock(LearningDirectionRepository.class)
        );

        when(questionRepository.findById(21L)).thenReturn(Optional.of(question(21L, 9L)));
        when(answerRepository.save(any(CommunityAnswer.class))).thenAnswer(invocation -> {
            CommunityAnswer answer = invocation.getArgument(0);
            ReflectionTestUtils.setField(answer, "id", 34L);
            return answer;
        });
        when(modelGateway.hasConfiguredModel()).thenReturn(false);

        CommunityAnswerResponse answer = service.createAnswer(9L, 21L, new CreateCommunityAnswerRequest("@ai 帮我解释一下"));

        assertThat(answer.source()).isEqualTo("AI");
        assertThat(answer.authorName()).isEqualTo("AI 助教");
        assertThat(answer.content()).contains("Redis AOF 为什么会重写？");
    }

    @Test
    void listsQuestionsWithAnswerCountAndAuthorName() {
        CommunityQuestionRepository questionRepository = mock(CommunityQuestionRepository.class);
        CommunityAnswerRepository answerRepository = mock(CommunityAnswerRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        ModelGateway modelGateway = mock(ModelGateway.class);
        CommunityService service = new CommunityService(
                questionRepository,
                answerRepository,
                userRepository,
                modelGateway,
                mock(LearningPlanRepository.class),
                mock(LearningDirectionRepository.class)
        );

        when(questionRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(question(21L, 9L)));
        when(answerRepository.countByQuestionId(21L)).thenReturn(2L);
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of(user(9L, "小林")));

        List<CommunityQuestionResponse> questions = service.listQuestions();

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).answerCount()).isEqualTo(2);
        assertThat(questions.get(0).authorName()).isEqualTo("小林");
    }

    private AppUser user(Long id, String nickname) {
        AppUser user = new AppUser(nickname, nickname + "@example.com", "hash", "USER");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private CommunityQuestion question(Long id, Long userId) {
        CommunityQuestion question = new CommunityQuestion(
                userId,
                null,
                "Redis AOF 为什么会重写？",
                "学习持久化时不太理解 AOF 重写触发条件。",
                "Redis,持久化"
        );
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
