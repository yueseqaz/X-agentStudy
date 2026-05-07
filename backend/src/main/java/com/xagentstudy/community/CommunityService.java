package com.xagentstudy.community;

import com.xagentstudy.agent.model.ModelGateway;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommunityService {
    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final AppUserRepository userRepository;
    private final ModelGateway modelGateway;

    public CommunityService(
            CommunityQuestionRepository questionRepository,
            CommunityAnswerRepository answerRepository,
            AppUserRepository userRepository,
            ModelGateway modelGateway
    ) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.modelGateway = modelGateway;
    }

    @Transactional(readOnly = true)
    public List<CommunityQuestionResponse> listQuestions() {
        List<CommunityQuestion> questions = questionRepository.findAllByOrderByUpdatedAtDesc();
        Map<Long, String> authorNames = authorNames(questions.stream().map(CommunityQuestion::getUserId).toList());
        return questions.stream()
                .map(question -> CommunityQuestionResponse.summary(
                        question,
                        authorNames.getOrDefault(question.getUserId(), "社区用户"),
                        (int) answerRepository.countByQuestionId(question.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityQuestionResponse getQuestion(Long questionId) {
        CommunityQuestion question = findQuestion(questionId);
        List<CommunityAnswer> answers = answerRepository.findByQuestionIdOrderByCreatedAtAsc(questionId);
        Map<Long, String> authorNames = authorNames(answers.stream()
                .map(CommunityAnswer::getUserId)
                .filter(id -> id != null)
                .toList());
        String questionAuthor = authorNames(List.of(question.getUserId())).getOrDefault(question.getUserId(), "社区用户");
        List<CommunityAnswerResponse> answerResponses = answers.stream()
                .map(answer -> CommunityAnswerResponse.from(answer, answerAuthorName(answer, authorNames)))
                .toList();
        return CommunityQuestionResponse.detail(question, questionAuthor, answerResponses);
    }

    @Transactional
    public CommunityQuestionResponse createQuestion(Long userId, CreateCommunityQuestionRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        CommunityQuestion saved = questionRepository.save(new CommunityQuestion(
                userId,
                request.title().trim(),
                request.content().trim(),
                normalizeTags(request.tags())
        ));
        return CommunityQuestionResponse.summary(saved, user.getNickname(), 0);
    }

    @Transactional
    public CommunityAnswerResponse createAnswer(Long userId, Long questionId, CreateCommunityAnswerRequest request) {
        String content = request.content().trim();
        if (content.toLowerCase().contains("@ai")) {
            return createAiAnswer(questionId, content);
        }
        CommunityQuestion question = findQuestion(questionId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        CommunityAnswer saved = answerRepository.save(new CommunityAnswer(questionId, userId, "USER", content));
        question.touch();
        questionRepository.save(question);
        return CommunityAnswerResponse.from(saved, user.getNickname());
    }

    @Transactional
    public CommunityAnswerResponse createAiAnswer(Long questionId, String mention) {
        CommunityQuestion question = findQuestion(questionId);
        List<CommunityAnswer> answers = answerRepository.findByQuestionIdOrderByCreatedAtAsc(questionId);
        String content = aiAnswer(question, answers, mention);
        CommunityAnswer saved = answerRepository.save(new CommunityAnswer(questionId, null, "AI", content));
        question.touch();
        questionRepository.save(question);
        return CommunityAnswerResponse.from(saved, "AI 助教");
    }

    private CommunityQuestion findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Community question not found"));
    }

    private String aiAnswer(CommunityQuestion question, List<CommunityAnswer> answers, String mention) {
        if (modelGateway.hasConfiguredModel()) {
            String userPrompt = renderPrompt("prompts/community-answer.md", Map.of(
                    "title", question.getTitle(),
                    "content", question.getContent(),
                    "answers", answersText(answers),
                    "mention", mention == null ? "请回答这个问题" : mention
            ));
            Optional<String> generated = modelGateway.generateText(systemPrompt(), userPrompt)
                    .map(String::trim)
                    .filter(value -> !value.isBlank());
            if (generated.isPresent()) {
                return generated.get();
            }
        }
        return "针对「" + question.getTitle() + "」，建议先把问题拆成概念、触发条件和例子三部分来理解。"
                + "如果这是课程学习中的疑问，可以补充你当前卡住的具体步骤，社区成员或 AI 可以继续给出更精确的解释。";
    }

    private String answersText(List<CommunityAnswer> answers) {
        if (answers.isEmpty()) {
            return "暂无回答";
        }
        return answers.stream()
                .map(answer -> answer.getSource() + "：" + answer.getContent())
                .collect(Collectors.joining("\n"));
    }

    private Map<Long, String> authorNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> names.put(user.getId(), user.getNickname()));
        return names;
    }

    private String answerAuthorName(CommunityAnswer answer, Map<Long, String> authorNames) {
        if ("AI".equalsIgnoreCase(answer.getSource())) {
            return "AI 助教";
        }
        return answer.getUserId() == null ? "社区用户" : authorNames.getOrDefault(answer.getUserId(), "社区用户");
    }

    private String normalizeTags(String tags) {
        return tags == null || tags.isBlank() ? "" : tags.trim();
    }

    private String systemPrompt() {
        return promptOrDefault("prompts/community-answer-system.md", "你是问答社区里的 AI 助教，回答要简洁、准确、可执行。");
    }

    private String renderPrompt(String path, Map<String, Object> variables) {
        String template = promptOrDefault(path, "");
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    private String promptOrDefault(String path, String fallback) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                return fallback;
            }
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
