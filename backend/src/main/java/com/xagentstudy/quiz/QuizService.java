package com.xagentstudy.quiz;

import com.xagentstudy.agent.orchestration.AgentGenerationService;
import com.xagentstudy.agent.orchestration.GeneratedQuizQuestion;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.rag.chunk.KnowledgeChunkResponse;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import com.xagentstudy.rag.chunk.SourceLabelService;
import com.xagentstudy.review.ReviewRecord;
import com.xagentstudy.review.ReviewRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final KnowledgeChunkService chunkService;
    private final QuestionRepository questionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final AgentGenerationService agentGenerationService;
    private final ReviewRecordRepository reviewRecordRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final SourceLabelService sourceLabelService;

    public QuizService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            KnowledgeChunkService chunkService,
            QuestionRepository questionRepository,
            AnswerRecordRepository answerRecordRepository,
            AgentGenerationService agentGenerationService,
            ReviewRecordRepository reviewRecordRepository,
            KnowledgeDocumentRepository documentRepository,
            ObjectMapper objectMapper,
            SourceLabelService sourceLabelService
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.chunkService = chunkService;
        this.questionRepository = questionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.agentGenerationService = agentGenerationService;
        this.reviewRecordRepository = reviewRecordRepository;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
        this.sourceLabelService = sourceLabelService;
    }

    @Transactional
    public List<QuestionResponse> generate(Long planId, GenerateQuizRequest request) {
        LearningPlan plan = ensurePlan(planId);
        String quizBatchId = java.util.UUID.randomUUID().toString();
        List<KnowledgeChunkResponse> chunks = request.documentId() == null
                ? chunkService.listByPlan(planId)
                : chunkService.listByDocument(planId, request.documentId());
        if (chunks.isEmpty()) {
            return generateColdStartQuestions(plan, request, quizBatchId);
        }

        int count = request.safeCount();
        List<KnowledgeChunkResponse> selectedChunks = chunks.stream()
                .limit(Math.min(count, Math.max(chunks.size(), 1)))
                .toList();
        String context = selectedChunks.stream()
                .map(chunk -> "Document #" + chunk.documentId() + " / " + chunk.sourceLocation() + "\n" + chunk.content())
                .collect(Collectors.joining("\n\n---\n\n"));

        Optional<List<GeneratedQuizQuestion>> generated = agentGenerationService.generateQuizQuestions(
                plan.getTitle(),
                request.safeDifficulty(),
                request.safeQuestionType(),
                count,
                "KNOWLEDGE_ENHANCED",
                documentContextPrefix(request) + context
        );
        if (generated.isPresent()) {
            String scope = sourceScope(request, selectedChunks);
            List<QuestionResponse> saved = saveGeneratedQuestions(planId, scope, quizBatchId, generated.get().stream().limit(count).toList());
            if (saved.size() >= count) {
                return saved;
            }
            java.util.List<QuestionResponse> filled = new java.util.ArrayList<>(saved);
            for (int index = saved.size(); index < count; index++) {
                KnowledgeChunkResponse chunk = selectedChunks.get(index % selectedChunks.size());
                filled.add(toResponse(questionRepository.save(buildFallbackKnowledgeQuestion(
                        planId,
                        request.safeDifficulty(),
                        request.safeQuestionType(),
                        chunk,
                        request.knowledgePoint(),
                        scope,
                        quizBatchId,
                        index
                ))));
            }
            return filled;
        }

        String scope = sourceScope(request, selectedChunks);
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> questionRepository.save(buildFallbackKnowledgeQuestion(
                        planId,
                        request.safeDifficulty(),
                        request.safeQuestionType(),
                        selectedChunks.get(index % selectedChunks.size()),
                        request.knowledgePoint(),
                        scope,
                        quizBatchId,
                        index
                )))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(Long planId) {
        ensurePlan(planId);
        return questionRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listWrong(Long planId) {
        ensurePlan(planId);
        List<Question> questions = questionRepository.findByPlanIdOrderByCreatedAtDesc(planId);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        if (questionIds.isEmpty()) {
            return List.of();
        }
        List<AnswerRecord> attempts = answerRecordRepository
                .findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(questionIds, AuthContext.currentUserId());
        java.util.Map<Long, AnswerRecord> latestByQuestion = new java.util.LinkedHashMap<>();
        for (AnswerRecord attempt : attempts) {
            latestByQuestion.putIfAbsent(attempt.getQuestionId(), attempt);
        }
        return questions.stream()
                .filter(question -> latestByQuestion.containsKey(question.getId()))
                .filter(question -> !Boolean.TRUE.equals(latestByQuestion.get(question.getId()).getCorrect()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listDocumentQuestions(Long planId, Long documentId) {
        ensurePlan(planId);
        return questionRepository.findByPlanIdAndSourceScopeOrderByCreatedAtDesc(planId, "DOCUMENT:" + documentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnswerRecordResponse answer(Long planId, Long questionId, SubmitAnswerRequest request) {
        ensurePlan(planId);
        Question question = questionRepository.findById(questionId)
                .filter(item -> item.getPlanId().equals(planId))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Question not found"));
        String answer = normalizeAnswer(question, request.answer());
        GradedAnswer graded = grade(question, answer);
        AnswerRecord saved = answerRecordRepository.save(new AnswerRecord(
                questionId,
                AuthContext.currentUserId(),
                toJson(List.of(answer)),
                graded.correct(),
                graded.score(),
                graded.feedback(),
                request.redoOfQuestionId()
        ));
        try {
            syncReviewAfterAnswer(planId, question, graded.correct());
            syncDocumentStatusAfterAnswer(planId, question);
        } catch (RuntimeException ignored) {
            // 答题记录是主链路；复习和文档状态联动失败时不能阻塞用户提交。
        }
        return AnswerRecordResponse.from(saved);
    }

    private List<QuestionResponse> saveGeneratedQuestions(Long planId, String sourceScope, String quizBatchId, List<GeneratedQuizQuestion> generatedQuestions) {
        return generatedQuestions.stream()
                .map(question -> questionRepository.save(new Question(
                        planId,
                        sourceScope,
                        quizBatchId,
                        normalizeType(question.type()),
                        normalizeDifficulty(question.difficulty()),
                        question.stem(),
                        question.options(),
                        question.standardAnswer(),
                        question.explanation(),
                        question.knowledgePoints()
                )))
                .map(this::toResponse)
                .toList();
    }

    private Question buildFallbackKnowledgeQuestion(
            Long planId,
            String difficulty,
            String questionType,
            KnowledgeChunkResponse chunk,
            String knowledgePoint,
            String sourceScope,
            String quizBatchId,
            int index
    ) {
        String type = normalizeType(questionType);
        String point = knowledgePoint == null || knowledgePoint.isBlank() ? "当前知识点" : knowledgePoint.trim();
        String snippet = compactSnippet(chunk.content());
        String stem = switch (type) {
            case "FILL_BLANK" -> "请填空：根据当前学习文档，「" + point + "」这一节主要围绕____展开。（第 " + (index + 1) + " 题）";
            case "SHORT_ANSWER" -> "请用自己的话概括当前文档中「" + point + "」的核心含义。（第 " + (index + 1) + " 题）";
            case "CODE" -> "请结合当前文档内容，用伪代码或命令描述一个与「" + point + "」相关的操作或验证步骤。（第 " + (index + 1) + " 题）";
            case "CASE_ANALYSIS" -> "案例分析：如果学习者已经阅读「" + point + "」但仍说不清使用场景，请结合当前文档分析应补充哪些理解。（第 " + (index + 1) + " 题）";
            case "INTERVIEW" -> "面试题：请根据当前文档说明「" + point + "」的定义、应用场景和常见误区。（第 " + (index + 1) + " 题）";
            default -> "根据当前学习文档，以下哪一项最符合「" + point + "」这一知识片段的核心意思？（第 " + (index + 1) + " 题）";
        };
        String optionA = "文档强调：" + snippet;
        String options = toJson(List.of(
                java.util.Map.of("key", "A", "text", optionA),
                java.util.Map.of("key", "B", "text", "该知识点与当前文档内容无关，可以只按通用学习方法理解。"),
                java.util.Map.of("key", "C", "text", "只需要记住标题，不需要理解定义、场景或误区。"),
                java.util.Map.of("key", "D", "text", "该知识点只能通过背诵完成，不需要结合示例或练习。")
        ));
        String explanation = "资料片段来自 Document #" + chunk.documentId() + " / " + chunk.sourceLocation()
                + "。题目围绕知识点「" + point + "」和该片段内容生成：" + snippet;
        return new Question(
                planId,
                sourceScope,
                quizBatchId,
                type,
                difficulty,
                stem,
                "SINGLE_CHOICE".equals(type) ? options : "[]",
                "SINGLE_CHOICE".equals(type) ? "[\"A\"]" : toJson(List.of("回答应结合当前文档说明「" + point + "」的定义、应用场景、示例或常见误区。")),
                explanation,
                toJson(List.of(point))
        );
    }

    private List<QuestionResponse> generateColdStartQuestions(LearningPlan plan, GenerateQuizRequest request, String quizBatchId) {
        int count = request.safeCount();
        Optional<List<GeneratedQuizQuestion>> generated = agentGenerationService.generateQuizQuestions(
                plan.getTitle(),
                request.safeDifficulty(),
                request.safeQuestionType(),
                count,
                "COLD_START",
                "当前计划暂无已解析知识库资料。请基于学习计划主题生成通用但可执行的练习题，帮助用户立即体验 AI 测验能力。"
        );
        if (generated.isPresent()) {
            return saveGeneratedQuestions(plan.getId(), "COLD_START", quizBatchId, generated.get());
        }

        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> questionRepository.save(buildColdStartQuestion(plan, request.safeDifficulty(), request.safeQuestionType(), quizBatchId, index)))
                .map(this::toResponse)
                .toList();
    }

    private Question buildColdStartQuestion(LearningPlan plan, String difficulty, String questionType, String quizBatchId, int index) {
        String type = normalizeType(questionType);
        String stem = switch (index % 3) {
            case 0 -> "在开始学习「" + plan.getTitle() + "」时，以下哪种做法最有利于建立稳定学习闭环？";
            case 1 -> "如果你暂时没有上传学习资料，使用 AI 学习时最应该先明确什么？";
            default -> "遇到一个新概念时，哪种练习方式最能检验是否真正掌握？";
        };
        if (!"SINGLE_CHOICE".equals(type)) {
            stem = switch (type) {
                case "FILL_BLANK" -> "请填空：学习闭环通常包括目标、计划、练习、反馈和____。";
                case "SHORT_ANSWER" -> "请简要说明开始学习「" + plan.getTitle() + "」时如何建立学习闭环。";
                case "CODE" -> "请用伪代码描述一个“学习任务完成后生成复习提醒”的流程。";
                case "CASE_ANALYSIS" -> "案例分析：用户只收藏资料但不做练习，学习效果差。请分析原因并给出调整方案。";
                case "INTERVIEW" -> "面试模拟：如何判断一个学习计划是否真正可执行？";
                default -> stem;
            };
        }
        String options = switch (index % 3) {
            case 0 -> """
                    [
                      {"key":"A","text":"先明确目标、拆分阶段任务，并用问答、测验和复习持续反馈。"},
                      {"key":"B","text":"一次性收集尽可能多的资料，暂时不做练习。"},
                      {"key":"C","text":"只看视频，不记录问题和错题。"},
                      {"key":"D","text":"等资料全部准备齐全后再开始学习。"}
                    ]
                    """;
            case 1 -> """
                    [
                      {"key":"A","text":"明确学习目标、当前基础、时间投入和希望解决的具体问题。"},
                      {"key":"B","text":"只要求 AI 给出越长越好的内容。"},
                      {"key":"C","text":"跳过计划，直接背诵零散概念。"},
                      {"key":"D","text":"只上传资料，不进行提问和练习。"}
                    ]
                    """;
            default -> """
                    [
                      {"key":"A","text":"用自己的话解释概念，并完成一个最小可运行练习。"},
                      {"key":"B","text":"只判断自己是否看懂了标题。"},
                      {"key":"C","text":"复制答案，不检查原因。"},
                      {"key":"D","text":"只收藏资料，暂时不输出。"}
                    ]
                    """;
        };
        String explanation = "这是一道无知识库模式下的通用练习题，未引用计划资料。上传资料并解析成功后，测验会优先基于资料内容生成。";
        return new Question(
                plan.getId(),
                "COLD_START",
                quizBatchId,
                type,
                difficulty,
                stem,
                "SINGLE_CHOICE".equals(type) ? options : "[]",
                "SINGLE_CHOICE".equals(type) ? "[\"A\"]" : "[\"回答应包含目标拆解、练习反馈、错题复盘和复习节奏。\"]",
                explanation,
                "[\"无知识库模式\", \"学习方法\", \"练习反馈\"]"
        );
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private String normalizeAnswer(Question question, String answer) {
        String normalized = answer == null ? "" : answer.trim();
        String type = normalizeType(question.getType());
        if ("SINGLE_CHOICE".equals(type) || "FILL_BLANK".equals(type)) {
            return normalized.toUpperCase();
        }
        return normalized;
    }

    private GradedAnswer grade(Question question, String normalizedAnswer) {
        String type = normalizeType(question.getType());
        if ("SINGLE_CHOICE".equals(type) || "FILL_BLANK".equals(type)) {
            List<String> standardAnswers = parseStringList(question.getStandardAnswer()).stream()
                    .map(answer -> answer == null ? "" : answer.trim().toUpperCase())
                    .filter(answer -> !answer.isBlank())
                    .toList();
            boolean exactMatch = standardAnswers.stream().anyMatch(answer -> answer.equals(normalizedAnswer));
            boolean fillBlankAccepted = "FILL_BLANK".equals(type)
                    && !normalizedAnswer.isBlank()
                    && standardAnswers.stream().anyMatch(answer -> answer.contains(normalizedAnswer) || normalizedAnswer.contains(answer));
            boolean correct = exactMatch || fillBlankAccepted;
            return new GradedAnswer(
                    correct,
                    correct ? 100 : 0,
                    objectiveFeedback(correct, standardAnswers, question.getExplanation())
            );
        }
        return agentGenerationService.gradeSubjectiveAnswer(type, question.getStem(), question.getStandardAnswer(), normalizedAnswer);
    }

    private String objectiveFeedback(boolean correct, List<String> standardAnswers, String explanation) {
        return toJson(java.util.Map.of(
                "overall", correct ? "回答正确。" : "回答不匹配，请对照标准答案和解析复盘。",
                "score", correct ? 100 : 0,
                "correctAnswer", standardAnswers,
                "explanation", explanation == null ? "" : explanation,
                "suggestions", correct
                        ? List.of("继续完成同知识点的下一题，确认掌握稳定。")
                        : List.of("先复述标准答案。", "回到来源文档定位相关段落。", "重做同知识点题目。")
        ));
    }

    private void syncReviewAfterAnswer(Long planId, Question question, boolean correct) {
        List<String> points = parseStringList(question.getKnowledgePoints());
        int priority = Math.max(1, points.size());
        for (String point : points) {
            String safePoint = truncate(point, 120);
            String reason = "来自题目 #" + question.getId() + " 的作答结果，来源：" + sourceLabelService.label(question.getSourceScope()) + "。";
            int currentPriority = priority;
            ReviewRecord record = reviewRecordRepository
                    .findFirstByPlanIdAndUserIdAndKnowledgePointOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(planId, AuthContext.currentUserId(), safePoint)
                    .orElseGet(() -> reviewRecordRepository.save(new ReviewRecord(
                            planId,
                            AuthContext.currentUserId(),
                            safePoint,
                            reason,
                            currentPriority
                    )));
            if (correct) {
                record.complete(5);
            } else {
                record.reopen(reason, currentPriority);
            }
            priority = Math.max(1, priority - 1);
        }
    }

    private void syncDocumentStatusAfterAnswer(Long planId, Question answeredQuestion) {
        String sourceScope = answeredQuestion.getSourceScope();
        if (sourceScope == null || !sourceScope.startsWith("DOCUMENT:")) {
            return;
        }
        Long documentId = parseLong(sourceScope.substring("DOCUMENT:".length()));
        if (documentId == null) {
            return;
        }
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(item -> item.getPlanId().equals(planId))
                .orElse(null);
        if (document == null) {
            return;
        }
        List<Question> documentQuestions = answeredQuestion.getQuizBatchId() == null || answeredQuestion.getQuizBatchId().isBlank()
                ? questionRepository.findByPlanIdAndSourceScopeOrderByCreatedAtDesc(planId, sourceScope)
                : questionRepository.findByPlanIdAndSourceScopeAndQuizBatchIdOrderByCreatedAtDesc(
                        planId,
                        sourceScope,
                        answeredQuestion.getQuizBatchId()
                );
        if (documentQuestions.isEmpty()) {
            document.updateLearningStatus("LEARNING");
            return;
        }
        List<Long> questionIds = documentQuestions.stream().map(Question::getId).toList();
        List<AnswerRecord> attempts = answerRecordRepository
                .findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(questionIds, AuthContext.currentUserId());
        java.util.Map<Long, AnswerRecord> latestByQuestion = new java.util.LinkedHashMap<>();
        for (AnswerRecord attempt : attempts) {
            latestByQuestion.putIfAbsent(attempt.getQuestionId(), attempt);
        }
        if (latestByQuestion.isEmpty()) {
            document.updateLearningStatus("LEARNING");
            return;
        }
        double coverage = latestByQuestion.size() * 1.0 / documentQuestions.size();
        double averageScore = latestByQuestion.values().stream()
                .mapToInt(record -> record.getScore() == null ? (Boolean.TRUE.equals(record.getCorrect()) ? 100 : 0) : record.getScore())
                .average()
                .orElse(0);
        boolean hasWrong = latestByQuestion.values().stream().anyMatch(record -> !Boolean.TRUE.equals(record.getCorrect()));
        if (coverage >= 0.8 && averageScore >= 85 && !hasWrong) {
            document.updateLearningStatus("COMPLETED");
        } else if (coverage >= 0.8 && averageScore >= 80) {
            document.updateLearningStatus("MASTERED");
        } else if (coverage >= 0.5 && averageScore >= 60) {
            document.updateLearningStatus("TESTED");
        } else {
            document.updateLearningStatus("NEEDS_REVIEW");
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "未标记知识点";
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return "未标记知识点";
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private List<String> parseStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.from(question, sourceLabelService.label(question.getSourceScope()));
    }

    private String compactSnippet(String content) {
        String snippet = content == null ? "" : content
                .replace("\r\n", "\n")
                .replaceAll("[#*`>\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (snippet.isBlank()) {
            return "当前文档围绕该知识点的定义、应用场景、示例和误区展开。";
        }
        return snippet.length() > 90 ? snippet.substring(0, 90) + "..." : snippet;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String sourceScope(List<KnowledgeChunkResponse> selectedChunks) {
        if (selectedChunks.size() == 1) {
            return "CHUNK:" + selectedChunks.get(0).id();
        }
        return "RAG:" + selectedChunks.stream()
                .map(KnowledgeChunkResponse::id)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String sourceScope(GenerateQuizRequest request, List<KnowledgeChunkResponse> selectedChunks) {
        if (request.documentId() != null) {
            return "DOCUMENT:" + request.documentId();
        }
        return sourceScope(selectedChunks);
    }

    private String documentContextPrefix(GenerateQuizRequest request) {
        if (request.knowledgePoint() == null || request.knowledgePoint().isBlank()) {
            return "";
        }
        return "当前测验必须围绕知识点：「" + request.knowledgePoint().trim() + "」。题目应直接检验用户是否读懂该知识点文档。\n\n";
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "SINGLE_CHOICE";
        }
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "SINGLE_CHOICE", "FILL_BLANK", "SHORT_ANSWER", "CODE", "CASE_ANALYSIS", "INTERVIEW" -> normalized;
            default -> "SINGLE_CHOICE";
        };
    }

    private String normalizeDifficulty(String difficulty) {
        return difficulty == null || difficulty.isBlank() ? "MEDIUM" : difficulty;
    }
}
