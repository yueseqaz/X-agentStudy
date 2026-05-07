package com.xagentstudy.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.model.ModelGateway;
import com.xagentstudy.billing.BillingService;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.profile.GenerateProfileRequest;
import com.xagentstudy.profile.LearningProfile;
import com.xagentstudy.quiz.GradedAnswer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgentGenerationService {
    private final ModelGateway modelGateway;
    private final ObjectMapper objectMapper;
    private final BillingService billingService;

    public AgentGenerationService(ModelGateway modelGateway, ObjectMapper objectMapper, BillingService billingService) {
        this.modelGateway = modelGateway;
        this.objectMapper = objectMapper;
        this.billingService = billingService;
    }

    public GeneratedProfile generateProfile(LearningDirection direction, GenerateProfileRequest request) {
        billingService.consumeAgentCall("PROFILE_AGENT");
        String answers = profileAnswers(request);
        String userPrompt = renderPrompt("prompts/profile.md", mapOf(
                "directionName", direction.getName(),
                "category", nullToBlank(direction.getCategory()),
                "description", nullToBlank(direction.getDescription()),
                "answers", answers
        ));

        return modelGateway.generateJson(profileSystemPrompt(), userPrompt)
                .flatMap(this::parseProfile)
                .orElseGet(() -> fallbackProfile(direction, request));
    }

    public GeneratedPlan generatePlan(LearningDirection direction, LearningProfile profile) {
        billingService.consumeAgentCall("PLAN_AGENT");
        String userPrompt = renderPrompt("prompts/plan-course.md", mapOf(
                "topic", direction.getName(),
                "description", nullToBlank(direction.getDescription()),
                "level", profile.getCurrentLevel(),
                "goal", profile.getGoal(),
                "timeBudget", profile.getTimeBudget(),
                "preference", profile.getPreference(),
                "risks", profile.getRisks(),
                "strategy", profile.getStrategy()
        ));

        return modelGateway.generateJson(planSystemPrompt(), userPrompt)
                .flatMap(json -> parsePlan(json, direction, profile))
                .orElseGet(() -> fallbackPlan(direction, profile));
    }

    public String generateKnowledgeDocumentMarkdown(
            LearningDirection direction,
            LearningProfile profile,
            LearningPlanContext planContext,
            KnowledgePointContext pointContext,
            String documentStyle
    ) {
        billingService.consumeAgentCall("DOCUMENT_AGENT");
        if (!modelGateway.hasConfiguredModel()) {
            throw new BusinessException("MODEL_NOT_CONFIGURED", "文档生成需要先在管理后台配置可用模型 Key");
        }
        String styleInstruction = switch (documentStyle == null ? "" : documentStyle.trim().toUpperCase(Locale.ROOT)) {
            case "CONVERSATIONAL" -> "口语化讲解文档：像老师面对面讲课，表达更自然，多用类比和问答式解释，但仍保持结构清晰。";
            case "PRACTICAL" -> "实战练习文档：突出操作步骤、练习任务、验收标准和常见错误排查。";
            case "INTERVIEW" -> "面试复盘文档：突出高频问题、标准回答、追问方向和表达模板。";
            default -> "专业学习文档：表达严谨，概念定义清晰，结构完整，适合沉淀为知识库资料。";
        };
        String userPrompt = renderPrompt("prompts/document-lecture.md", mapOf(
                "planTitle", planContext.title(),
                "unitTitle", pointContext.unitName(),
                "chapterTitle", pointContext.chapterName(),
                "pointTitle", pointContext.title(),
                "pointSummary", pointContext.outcome(),
                "level", profile == null ? pointContext.level() : nullToBlank(profile.getCurrentLevel()),
                "documentStyle", styleInstruction
        ));

        return modelGateway.generateText(documentSystemPrompt(), userPrompt)
                .map(this::cleanMarkdownOutput)
                .filter(markdown -> !markdown.isBlank())
                .filter(markdown -> !looksLikeGenericKnowledgeDocument(markdown))
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_MODEL_GENERATION_FAILED",
                        "模型没有返回有效讲义内容，请检查模型网关配置或稍后重试"
                ));
    }

    public GeneratedAnswer generateAnswer(String question, String userLevel, String retrievedContext) {
        return generateAnswer(question, userLevel, "", retrievedContext);
    }

    public GeneratedAnswer generateAnswer(String question, String userLevel, String planContext, String retrievedContext) {
        billingService.consumeAgentCall("QA_AGENT");
        String userPrompt = renderPrompt("prompts/tutor.md", mapOf(
                "question", question,
                "userLevel", nullToBlank(userLevel),
                "planContext", nullToBlank(planContext),
                "retrievedContext", nullToBlank(retrievedContext)
        ));

        return modelGateway.generateJson(tutorSystemPrompt(), userPrompt)
                .flatMap(this::parseAnswer)
                .orElseGet(() -> fallbackAnswer(question, planContext.isBlank() ? retrievedContext : planContext));
    }

    public Optional<List<GeneratedQuizQuestion>> generateQuizQuestions(
            String planTitle,
            String difficulty,
            String questionType,
            int count,
            String mode,
            String context
    ) {
        billingService.consumeAgentCall("QUIZ_AGENT");
        String userPrompt = renderPrompt("prompts/quiz.md", mapOf(
                "planTitle", nullToBlank(planTitle),
                "mode", nullToBlank(mode),
                "difficulty", nullToBlank(difficulty),
                "questionType", nullToBlank(questionType),
                "count", count,
                "context", nullToBlank(context)
        ));

        return modelGateway.generateJson(quizSystemPrompt(), userPrompt)
                .flatMap(json -> parseQuizQuestions(json, difficulty, count));
    }

    public GradedAnswer gradeSubjectiveAnswer(String questionType, String stem, String standardAnswer, String userAnswer) {
        billingService.consumeAgentCall("GRADER_AGENT");
        String userPrompt = renderPrompt("prompts/grader.md", mapOf(
                "questionType", nullToBlank(questionType),
                "stem", nullToBlank(stem),
                "standardAnswer", nullToBlank(standardAnswer),
                "userAnswer", nullToBlank(userAnswer)
        ));

        return modelGateway.generateJson(graderSystemPrompt(), userPrompt)
                .flatMap(this::parseGrade)
                .orElseGet(() -> {
                    int score = Math.min(100, Math.max(0, userAnswer == null ? 0 : userAnswer.trim().length() * 3));
                    return new GradedAnswer(score >= 60, score, score >= 60 ? "回答具备基本完整度，建议继续补充关键概念和例子。" : "回答较短，建议补充关键要点、原因和一个例子。");
                });
    }

    public SummaryCardDraft generateSummaryCard(Long planId, String planTitle, Long documentId, String documentName, String documentContent) {
        billingService.consumeAgentCall("SUMMARY_CARD_AGENT");
        String userPrompt = renderPrompt("prompts/summary-card.md", mapOf(
                "planId", planId,
                "planTitle", nullToBlank(planTitle),
                "sourceDocumentId", documentId,
                "sourceDocumentName", nullToBlank(documentName),
                "documentContent", nullToBlank(documentContent)
        ));

        return modelGateway.generateJson(summaryCardSystemPrompt(), userPrompt)
                .flatMap(json -> parseSummaryCard(json, planId, documentId, documentName))
                .orElseGet(() -> fallbackSummaryCard(planId, planTitle, documentId, documentName, documentContent));
    }

    public String profileAnswers(GenerateProfileRequest request) {
        return request.answers().stream()
                .map(answer -> answer.question() + "：" + answer.answer())
                .collect(Collectors.joining("\n"));
    }

    private String profileSystemPrompt() {
        return promptOrDefault("prompts/profile-system.md", "你是画像 Agent。你只输出 JSON，不输出 Markdown。内容要具体、可执行、少空话。");
    }

    private String planSystemPrompt() {
        return promptOrDefault("prompts/plan-system.md", "你是面向在线学习平台的课程规划 Agent。你只输出合法 JSON，不输出 Markdown。必须用课程设计思维生成计划、单元、知识点三层结构，知识点要具体、可生成文档、可出题、可统计掌握度。");
    }

    private String tutorSystemPrompt() {
        return promptOrDefault("prompts/tutor-system.md", "你是答疑 Agent。你只输出 JSON，不输出 Markdown。支持无知识库模式和知识库增强模式；有资料时优先引用资料，无资料时必须仍然给出通用讲解并说明未引用计划资料。");
    }

    private String quizSystemPrompt() {
        return promptOrDefault("prompts/quiz-system.md", "你是测验 Agent。你只输出 JSON，不输出 Markdown。题目要可作答、有唯一标准答案、解析具体，并服务于长期学习闭环。");
    }

    private String graderSystemPrompt() {
        return promptOrDefault("prompts/grader-system.md", "你是测验批改 Agent。你只输出 JSON，不输出 Markdown。对主观题、代码题、案例题、面试题进行严格但有帮助的评分。");
    }

    private String documentSystemPrompt() {
        return promptOrDefault("prompts/document-system.md", "你是专业课程讲义编写助手。你只输出 Markdown 正文，不输出 JSON，不输出解释性前后缀。内容必须围绕知识点本身展开，禁止学习方法套话。");
    }

    private String summaryCardSystemPrompt() {
        return promptOrDefault("prompts/summary-card-system.md", "你是文档知识总结图内容提炼 Agent。你只输出合法 JSON，不输出 Markdown。内容要短句化、条目化，适合前端渲染成学习总结图。");
    }

    private java.util.Optional<GeneratedProfile> parseProfile(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return java.util.Optional.of(new GeneratedProfile(
                    text(root, "goal"),
                    text(root, "currentLevel"),
                    text(root, "timeBudget"),
                    text(root, "preference"),
                    root.path("risks").isArray() ? objectMapper.writeValueAsString(root.path("risks")) : "[]",
                    text(root, "strategy")
            ));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private Optional<SummaryCardDraft> parseSummaryCard(String json, Long planId, Long documentId, String documentName) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> highlights = stringList(root.path("highlights"), 6);
            List<String> keywords = stringList(root.path("keywords"), 8);
            List<String> tips = stringList(root.path("tips"), 3);
            String title = textOrDefault(root, "title", documentName + " 知识总结");
            String summary = textOrDefault(root, "summary", "这份总结提炼了文档中的核心定义、关键方法和学习提醒。");
            return Optional.of(new SummaryCardDraft(
                    title,
                    summary,
                    highlights.isEmpty() ? List.of("理解文档的核心概念", "梳理关键知识点", "记录需要复习的易错点") : highlights,
                    keywords.isEmpty() ? List.of("知识总结", "重点提炼", "复习") : keywords,
                    tips,
                    String.valueOf(documentId),
                    documentName,
                    String.valueOf(planId)
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private java.util.Optional<GeneratedPlan> parsePlan(String json, LearningDirection direction, LearningProfile profile) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String title = textOrDefault(root, "planTitle", text(root, "title"));
            String goal = textOrDefault(root, "overallGoal", text(root, "goal"));
            JsonNode unitsNode = root.path("units");
            if (unitsNode.isArray() && !unitsNode.isEmpty()) {
                String stagesJson = objectMapper.writeValueAsString(courseUnitsToStages(unitsNode));
                if (looksLikeGenericPlan(stagesJson)) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new GeneratedPlan(
                        title.isBlank() ? direction.getName() + " 系统学习计划" : title,
                        goal.isBlank() ? profile.getGoal() : goal,
                        stagesJson
                ));
            }
            JsonNode stagesNode = root.path("stages");
            if (!stagesNode.isArray() || stagesNode.isEmpty()) {
                return java.util.Optional.empty();
            }
            String stagesJson = objectMapper.writeValueAsString(stagesNode);
            if (looksLikeGenericPlan(stagesJson)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new GeneratedPlan(
                    title.isBlank() ? direction.getName() + " 系统学习计划" : title,
                    goal.isBlank() ? profile.getGoal() : goal,
                    stagesJson
            ));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private List<Map<String, Object>> courseUnitsToStages(JsonNode unitsNode) {
        List<Map<String, Object>> stages = new ArrayList<>();
        int unitIndex = 1;
        for (JsonNode unit : unitsNode) {
            String unitTitle = textOrDefault(unit, "unitTitle", "课程单元 " + unitIndex);
            String unitSummary = text(unit, "unitSummary");
            String unitGoal = textOrDefault(unit, "unitGoal", unitSummary);
            String difficulty = textOrDefault(unit, "difficulty", "基础");
            int estimatedHours = unit.path("estimatedHours").asInt(0);
            JsonNode pointsNode = unit.path("knowledgePoints");
            List<Map<String, Object>> points = new ArrayList<>();
            if (pointsNode.isArray()) {
                int pointIndex = 1;
                for (JsonNode point : pointsNode) {
                    String pointTitle = text(point, "pointTitle");
                    if (pointTitle.isBlank()) {
                        continue;
                    }
                    String pointDifficulty = textOrDefault(point, "difficulty", difficulty);
                    points.add(mapOf(
                            "id", "u" + unitIndex + "k" + pointIndex,
                            "title", pointTitle,
                            "level", courseDifficultyToLevel(pointDifficulty),
                            "outcome", textOrDefault(point, "pointSummary", "能理解并应用「" + pointTitle + "」。"),
                            "estimatedMinutes", estimatedMinutes(pointDifficulty, estimatedHours, pointsNode.size()),
                            "docGenerationPriority", textOrDefault(point, "docGenerationPriority", "中"),
                            "quizGenerationPriority", textOrDefault(point, "quizGenerationPriority", "中"),
                            "reviewPriority", textOrDefault(point, "reviewPriority", "中")
                    ));
                    pointIndex++;
                }
            }
            if (!points.isEmpty()) {
                stages.add(mapOf(
                        "chapterIndex", unitIndex,
                        "name", "第 " + unitIndex + " 单元：" + unitTitle,
                        "focus", unitSummary.isBlank() ? unitGoal : unitSummary,
                        "duration", estimatedHours > 0 ? estimatedHours + " 小时" : "1 周",
                        "outcome", unitGoal,
                        "tasks", List.of("完成本单元知识点学习", "生成并阅读核心学习文档", "完成本单元测验并复习错题"),
                        "units", List.of(mapOf(
                                "unitIndex", 1,
                                "name", unitTitle,
                                "goal", unitGoal,
                                "knowledgePoints", points
                        ))
                ));
                unitIndex++;
            }
        }
        return stages;
    }

    private String courseDifficultyToLevel(String difficulty) {
        String value = difficulty == null ? "" : difficulty;
        if (value.contains("高级")) {
            return "PROJECT";
        }
        if (value.contains("进阶")) {
            return "ADVANCED";
        }
        if (value.contains("入门")) {
            return "FOUNDATION";
        }
        return "CORE";
    }

    private int estimatedMinutes(String difficulty, int estimatedHours, int pointCount) {
        if (estimatedHours > 0 && pointCount > 0) {
            return Math.max(30, Math.min(120, Math.round(estimatedHours * 60.0f / pointCount)));
        }
        String value = difficulty == null ? "" : difficulty;
        if (value.contains("高级")) {
            return 90;
        }
        if (value.contains("进阶")) {
            return 75;
        }
        if (value.contains("入门")) {
            return 45;
        }
        return 60;
    }

    private java.util.Optional<GeneratedAnswer> parseAnswer(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return java.util.Optional.of(new GeneratedAnswer(
                    text(root, "answer"),
                    root.path("relatedPoints").isArray() ? objectMapper.writeValueAsString(root.path("relatedPoints")) : "[]",
                    root.path("suggestedQuestions").isArray() ? objectMapper.writeValueAsString(root.path("suggestedQuestions")) : "[]"
            ));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private Optional<String> parseMarkdownDocument(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String markdown = text(root, "markdown");
            return markdown.isBlank() ? Optional.empty() : Optional.of(markdown);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<List<GeneratedQuizQuestion>> parseQuizQuestions(String json, String fallbackDifficulty, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray() || questionsNode.isEmpty()) {
                return Optional.empty();
            }

            List<GeneratedQuizQuestion> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                String stem = text(node, "stem");
                if (stem.isBlank()) {
                    continue;
                }
                String type = textOrDefault(node, "type", "SINGLE_CHOICE");
                JsonNode optionsNode = node.path("options");
                JsonNode answerNode = node.path("standardAnswer");
                JsonNode pointsNode = node.path("knowledgePoints");
                if (!answerNode.isArray() || answerNode.isEmpty()) {
                    continue;
                }
                if ("SINGLE_CHOICE".equalsIgnoreCase(type) && (!optionsNode.isArray() || optionsNode.size() < 2)) {
                    continue;
                }
                questions.add(new GeneratedQuizQuestion(
                        type,
                        textOrDefault(node, "difficulty", fallbackDifficulty),
                        stem,
                        optionsNode.isArray() ? objectMapper.writeValueAsString(optionsNode) : "[]",
                        objectMapper.writeValueAsString(answerNode),
                        textOrDefault(node, "explanation", "这道题用于检验当前学习计划中的相关知识点。"),
                        pointsNode.isArray() ? objectMapper.writeValueAsString(pointsNode) : "[]"
                ));
                if (questions.size() >= expectedCount) {
                    break;
                }
            }

            return questions.isEmpty() ? Optional.empty() : Optional.of(questions);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<GradedAnswer> parseGrade(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            int score = Math.max(0, Math.min(100, root.path("score").asInt()));
            String feedback = objectMapper.writeValueAsString(mapOf(
                    "overall", textOrDefault(root, "overall", textOrDefault(root, "feedback", "已完成批改。")),
                    "strengths", stringArray(root.path("strengths")),
                    "issues", stringArray(root.path("issues")),
                    "suggestions", stringArray(root.path("suggestions")),
                    "referenceAnswer", textOrDefault(root, "referenceAnswer", "")
            ));
            return Optional.of(new GradedAnswer(
                    root.path("correct").asBoolean(score >= 60),
                    score,
                    feedback
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private List<String> stringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.asText("").isBlank()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private GeneratedProfile fallbackProfile(LearningDirection direction, GenerateProfileRequest request) {
        String goal = pickAnswer(request, 0, "围绕「" + direction.getName() + "」建立可执行的技能学习闭环");
        String currentLevel = inferLevel(pickAnswer(request, 1, ""));
        String timeBudget = pickAnswer(request, 2, "每周 5-8 小时");
        String preference = pickAnswer(request, 3, "项目驱动、示例讲解、阶段复盘");
        String risks = "[\"学习目标过大导致执行分散\", \"资料过多但缺少练习反馈\", \"复习节奏不足\"]";
        String strategy = "先用 1-2 周建立基础概念和环境，再按阶段推进项目练习；每个阶段都绑定资料、问答、测验和复习，避免只看不练。";
        return new GeneratedProfile(goal, currentLevel, timeBudget, preference, risks, strategy);
    }

    private GeneratedPlan fallbackPlan(LearningDirection direction, LearningProfile profile) {
        String stages = fallbackChaptersJson(direction);
        return new GeneratedPlan(direction.getName() + " 系统学习计划", profile.getGoal(), stages);
    }

    private boolean looksLikeGenericPlan(String stagesJson) {
        String text = stagesJson == null ? "" : stagesJson;
        return List.of(
                        "学习起点与目标拆解",
                        "能力边界",
                        "第一个最小练习",
                        "资料增强学习",
                        "长期复盘",
                        "复盘规划",
                        "下一阶段",
                        "综合提升",
                        "掌握基础",
                        "核心模块一",
                        "核心模块二"
                )
                .stream()
                .anyMatch(text::contains);
    }

    private String fallbackChaptersJson(LearningDirection direction) {
        String name = direction.getName();
        String lowerName = name.toLowerCase(Locale.ROOT);
        String[][] chapters = lowerName.contains("springboot") || lowerName.contains("spring boot")
                ? new String[][]{
                {"Spring Boot 项目启动与工程结构", "理解 Spring Boot 的定位、启动流程和标准项目结构。", "项目基础", "Spring Boot 与 Spring Framework 的关系", "Maven/Gradle 项目结构", "启动类与自动扫描", "application.yml 基础配置"},
                {"Web 接口与请求处理", "掌握 REST API 的路由、参数绑定、响应结构和异常处理。", "Web 开发", "Controller 与 RequestMapping", "路径参数与请求体绑定", "DTO 与参数校验", "统一响应结构", "全局异常处理"},
                {"依赖注入与业务分层", "建立 Controller、Service、Repository 的清晰职责边界。", "分层设计", "Bean 生命周期与依赖注入", "Service 业务编排", "Repository 数据访问职责", "配置类与条件装配", "常见循环依赖问题"},
                {"数据访问与事务管理", "掌握 Spring Boot 中数据库连接、ORM、事务和分页查询。", "数据持久化", "数据源与连接池配置", "JPA/MyBatis 集成方式", "实体与表结构映射", "事务传播与回滚", "分页与条件查询"},
                {"接口安全与登录鉴权", "构建登录、权限控制和接口访问保护。", "安全机制", "Spring Security 过滤器链", "密码加密与登录认证", "JWT 生成与校验", "接口权限注解", "跨域与 CSRF 配置"},
                {"缓存、异步与任务调度", "引入缓存、异步执行和定时任务提升系统能力。", "系统增强", "Redis 缓存接入", "Cache 注解与失效策略", "异步任务 @Async", "定时任务 @Scheduled", "消息队列接入思路"},
                {"测试、部署与综合项目", "完成接口测试、配置隔离、构建部署和项目交付。", "工程交付", "单元测试与 MockMvc", "多环境配置", "日志与监控基础", "Docker 打包部署", "综合 CRUD 项目设计"}
        }
                : lowerName.contains("redis")
                ? new String[][]{
                {"Redis 基础与数据模型", "理解 Redis 的定位、内存模型和基础命令。", "基础模型", "Redis 的单线程事件模型", "Key 命名与过期时间", "String 数据结构", "Hash 数据结构", "List/Set/ZSet 使用场景"},
                {"缓存设计与一致性", "掌握业务系统中缓存读写、失效和一致性策略。", "缓存设计", "缓存穿透与布隆过滤器", "缓存击穿与互斥锁", "缓存雪崩与过期打散", "Cache Aside 模式", "双写一致性问题"},
                {"持久化与内存管理", "理解 RDB、AOF、淘汰策略和容量规划。", "可靠性", "RDB 快照机制", "AOF 日志机制", "混合持久化", "内存淘汰策略", "大 Key 与热 Key 识别"},
                {"高可用与集群", "掌握主从、哨兵、Cluster 的部署形态和故障转移。", "高可用", "主从复制流程", "哨兵选主机制", "Cluster 槽位与重定向", "故障转移过程", "集群扩容与迁移"},
                {"分布式场景应用", "学习 Redis 在锁、限流、排行榜和消息场景的使用方式。", "场景应用", "分布式锁与 Redisson", "计数器与限流", "排行榜与 ZSet", "延迟队列思路", "Session 与登录态存储"},
                {"性能调优与问题排查", "建立 Redis 线上问题定位和性能优化能力。", "运维调优", "慢查询分析", "Pipeline 与批量操作", "连接池配置", "阻塞命令风险", "监控指标与告警"}
        }
                : lowerName.contains("mysql")
                ? new String[][]{
                {"MySQL 表设计与 SQL 基础", "建立关系模型、表结构和基础查询能力。", "关系模型", "库表字段设计", "主键与外键", "SELECT 查询", "INSERT/UPDATE/DELETE", "聚合与分组"},
                {"索引原理与查询优化", "理解索引结构、执行计划和慢查询优化。", "索引优化", "B+Tree 索引结构", "联合索引最左前缀", "覆盖索引", "EXPLAIN 执行计划", "慢 SQL 优化步骤"},
                {"事务与锁机制", "掌握事务隔离、MVCC、锁和并发问题。", "事务并发", "ACID 与隔离级别", "MVCC 版本链", "行锁与间隙锁", "死锁产生与排查", "事务边界设计"},
                {"高级查询与数据建模", "处理复杂查询、分页、统计和业务建模。", "查询建模", "JOIN 连接查询", "子查询与派生表", "分页查询优化", "统计报表 SQL", "范式与反范式设计"},
                {"备份恢复与运维", "掌握备份、恢复、权限和基础运维操作。", "数据库运维", "用户与权限管理", "逻辑备份与恢复", "binlog 基础", "主从复制概念", "容量与归档策略"},
                {"项目实战与性能排查", "在真实业务接口中应用 MySQL 设计和优化。", "项目实战", "订单表设计", "高频查询索引设计", "数据一致性方案", "SQL Review 清单", "线上故障排查流程"}
        }
                : name.toLowerCase(Locale.ROOT).contains("java")
                ? new String[][]{
                {"Java 开发环境与程序结构", "建立 Java 后端开发所需的运行环境、项目结构和程序入口。", "开发环境", "JDK 与 JRE", "IDEA 项目结构", "main 方法与程序入口", "包名与类路径"},
                {"Java 基础语法", "掌握 Java 语言的变量、类型、表达式、流程控制和方法。", "语法基础", "变量与基本类型", "运算符与表达式", "if/switch 分支", "for/while 循环", "方法定义与参数传递"},
                {"面向对象编程", "理解类、对象、封装、继承、多态和接口抽象。", "对象模型", "类与对象", "构造方法", "封装与访问控制", "继承与方法重写", "接口与抽象类"},
                {"集合、泛型与常用 API", "掌握后端开发中高频使用的数据结构和标准库。", "集合框架", "List/Set/Map", "迭代器与 Stream", "泛型类型约束", "String 与日期时间 API", "Optional 与工具类"},
                {"异常处理与文件 IO", "掌握异常体系、资源关闭、文件读写和序列化基础。", "可靠性基础", "异常分类与捕获", "自定义异常", "try-with-resources", "字节流与字符流", "NIO 文件操作"},
                {"并发编程基础", "理解线程、线程池、锁、并发集合和异步任务。", "并发模型", "Thread 与 Runnable", "Executor 线程池", "synchronized 与 Lock", "ConcurrentHashMap", "CompletableFuture"},
                {"数据库与持久化", "掌握 SQL、表设计、事务和 Java 数据访问。", "数据访问", "表结构与主外键", "SQL 增删改查", "索引与事务", "JDBC 与连接池", "MyBatis 或 JPA 基础"},
                {"Spring Boot Web 开发", "构建 REST API，并完成分层、参数校验和统一异常处理。", "Web 服务", "Spring Boot 启动流程", "Controller 与路由", "DTO 与参数校验", "Service 与 Repository 分层", "统一响应与异常处理"},
                {"接口安全、缓存与消息", "理解真实后端系统中的登录鉴权、缓存和异步解耦。", "系统组件", "JWT 登录鉴权", "Spring Security 权限", "Redis 缓存", "消息队列基础", "幂等与限流"},
                {"测试、部署与综合项目", "完成接口测试、构建部署和一个可展示后端项目。", "交付实践", "JUnit 单元测试", "接口测试", "Maven 构建", "Docker 部署", "综合项目模块设计"}
        }
                : new String[][]{
                {name + " 学习对象与应用边界", "建立 " + name + " 的基本对象、术语和应用场景。", "入门认知", name + " 的定义与典型场景", name + " 的核心对象", name + " 的关键术语", name + " 的学习产出"},
                {name + " 基础概念与关系", "掌握该方向后续单元反复使用的基础概念。", "概念基础", name + " 的基础概念", name + " 的概念关系", name + " 的常见分类", name + " 的判断标准"},
                {name + " 标准流程与方法", "学习该方向中最常见的方法、流程和质量要求。", "方法流程", name + " 的标准流程", name + " 的常用方法", name + " 的输入输出", name + " 的质量检查"},
                {name + " 工具、材料与操作", "理解完成该方向学习所需的工具、资料和工作流。", "工具操作", name + " 的常用工具", name + " 的资料结构", name + " 的操作步骤", name + " 的产出格式"},
                {name + " 典型场景应用", "把基础知识放入真实场景中进行组合使用。", "场景应用", name + " 的典型场景", name + " 的方案设计", name + " 的执行步骤", name + " 的结果评估"},
                {name + " 常见问题与优化", "识别学习和实践中高频错误，并形成修正能力。", "问题优化", name + " 的常见误区", name + " 的错误定位", name + " 的优化方法", name + " 的复盘指标"},
                {name + " 综合项目实践", "完成一个覆盖主要单元的综合案例。", "综合实践", name + " 案例选题", name + " 案例结构", name + " 案例实现", name + " 案例验收"}
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < chapters.length; i++) {
            String[] chapter = chapters[i];
            List<Map<String, Object>> points = new ArrayList<>();
            for (int k = 3; k < chapter.length; k++) {
                points.add(mapOf(
                        "id", "c" + (i + 1) + "u1k" + (k - 2),
                        "title", chapter[k],
                        "level", i < 2 ? "FOUNDATION" : i < chapters.length - 2 ? "CORE" : "PROJECT",
                        "outcome", "能围绕「" + chapter[k] + "」完成讲解、练习和一次自测。",
                        "estimatedMinutes", 45 + (i % 3) * 15
                ));
            }
            points.add(mapOf(
                    "id", "c" + (i + 1) + "u2k1",
                    "title", chapter[2] + " 的案例应用",
                    "level", i < 2 ? "FOUNDATION" : "CORE",
                    "outcome", "能围绕本章内容完成一个贴近场景的案例应用。",
                    "estimatedMinutes", 60
            ));
            points.add(mapOf(
                    "id", "c" + (i + 1) + "u2k2",
                    "title", chapter[2] + " 的常见错误",
                    "level", "CORE",
                    "outcome", "能识别本章最常见错误并说明修正方式。",
                    "estimatedMinutes", 45
            ));
            points.add(mapOf(
                    "id", "c" + (i + 1) + "u2k3",
                    "title", chapter[2] + " 的测验题型",
                    "level", i < chapters.length - 2 ? "CORE" : "PROJECT",
                    "outcome", "能识别本章适合用哪些题型检验掌握情况。",
                    "estimatedMinutes", 45
            ));
            List<Map<String, Object>> conceptPoints = points.subList(0, Math.min(3, points.size()));
            List<Map<String, Object>> practicePoints = points.subList(Math.min(3, points.size()), points.size());
            result.add(mapOf(
                    "chapterIndex", i + 1,
                    "name", "第 " + (i + 1) + " 章：" + chapter[0],
                    "focus", chapter[1],
                    "duration", i < 2 ? "1 周" : "1-2 周",
                    "outcome", "完成本章后，用户可以掌握「" + chapter[0] + "」并产出可检查练习。",
                    "tasks", List.of("阅读本章目录并确认学习顺序", "完成本章所有知识点学习", "完成本章测验"),
                    "units", List.of(
                            mapOf(
                                    "unitIndex", 1,
                                    "name", chapter[2] + "：概念理解",
                                    "goal", "先理解本章最关键的概念和关系。",
                                    "knowledgePoints", conceptPoints
                            ),
                            mapOf(
                                    "unitIndex", 2,
                                    "name", chapter[2] + "：应用与测验",
                                    "goal", "通过案例应用和测验确认知识点真正掌握。",
                                    "knowledgePoints", practicePoints
                            )
                    )
            ));
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length - 1; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }

    private String renderPrompt(String classpathLocation, Map<String, Object> values) {
        String template = readPrompt(classpathLocation);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return template;
    }

    private String readPrompt(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String promptOrDefault(String classpathLocation, String fallback) {
        String prompt = readPrompt(classpathLocation).trim();
        return prompt.isBlank() ? fallback : prompt;
    }

    private String cleanMarkdownOutput(String markdown) {
        String value = markdown == null ? "" : markdown.trim();
        if (value.startsWith("```markdown")) {
            value = value.substring("```markdown".length()).trim();
        } else if (value.startsWith("```")) {
            value = value.substring(3).trim();
        }
        if (value.endsWith("```")) {
            value = value.substring(0, value.length() - 3).trim();
        }
        return value;
    }

    private boolean looksLikeGenericKnowledgeDocument(String markdown) {
        String text = markdown == null ? "" : markdown;
        List<String> genericMarkers = List.of(
                "这个知识点通常包含三个层面",
                "它通常承担三个作用",
                "输入、处理过程和输出",
                "先用一句话写下",
                "课程主题：",
                "当前知识点：",
                "典型场景：在一个真实项目或案例中",
                "不是孤立知识点",
                "用于定位薄弱点",
                "建议先记录错题"
        );
        int hit = 0;
        for (String marker : genericMarkers) {
            if (text.contains(marker)) {
                hit++;
            }
        }
        return hit >= 2;
    }

    private String fallbackKnowledgeDocument(
            LearningDirection direction,
            LearningProfile profile,
            LearningPlanContext planContext,
            KnowledgePointContext pointContext
    ) {
        String title = pointContext.title();
        String directionName = direction == null ? "" : nullToBlank(direction.getName()).toLowerCase(Locale.ROOT);

        if (directionName.contains("mysql")
                && title.toLowerCase(Locale.ROOT).contains("insert")
                && title.toLowerCase(Locale.ROOT).contains("update")
                && title.toLowerCase(Locale.ROOT).contains("delete")) {
            return """
                    # MySQL 知识文档：INSERT / UPDATE / DELETE

                    ## 一、学习目标
                    学完本节后，你应该能够：
                    - 理解 INSERT、UPDATE、DELETE 的作用与区别
                    - 向表中新增数据、修改已有数据、删除指定数据
                    - 理解 WHERE 条件在更新和删除中的关键作用
                    - 避免误操作导致整表数据被改写或删除

                    ## 二、前置知识
                    建议先掌握：
                    - MySQL 基础概念（数据库、表、字段、记录）
                    - SELECT 与 WHERE 的基础用法

                    ## 三、核心概念
                    在 MySQL 中：
                    - INSERT：新增记录
                    - UPDATE：修改记录
                    - DELETE：删除记录

                    三者属于 DML（Data Manipulation Language，数据操作语言）。

                    ## 四、示例表
                    ```sql
                    CREATE TABLE student (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(50),
                        age INT,
                        gender VARCHAR(10),
                        score DECIMAL(5,2)
                    );
                    ```

                    ## 五、INSERT：新增数据
                    基本语法：
                    ```sql
                    INSERT INTO 表名 (字段1, 字段2, 字段3)
                    VALUES (值1, 值2, 值3);
                    ```

                    示例：
                    ```sql
                    INSERT INTO student (name, age, gender, score)
                    VALUES ('张三', 20, '男', 88.5);
                    ```

                    批量插入：
                    ```sql
                    INSERT INTO student (name, age, gender, score)
                    VALUES
                    ('李四', 21, '男', 92.0),
                    ('王五', 19, '女', 95.5);
                    ```

                    注意：
                    - 字段和值数量必须一一对应
                    - 字符串要用单引号
                    - 主键冲突会报错

                    ## 六、UPDATE：更新数据
                    基本语法：
                    ```sql
                    UPDATE 表名
                    SET 字段1 = 值1, 字段2 = 值2
                    WHERE 条件;
                    ```

                    示例：
                    ```sql
                    UPDATE student
                    SET score = 90
                    WHERE name = '张三';
                    ```

                    批量更新（按条件）：
                    ```sql
                    UPDATE student
                    SET score = score + 5
                    WHERE score < 80;
                    ```

                    风险点：
                    ```sql
                    UPDATE student SET score = 100;
                    ```
                    这会修改整表数据。生产环境应先 `SELECT` 确认范围，再执行 `UPDATE`。

                    ## 七、DELETE：删除数据
                    基本语法：
                    ```sql
                    DELETE FROM 表名
                    WHERE 条件;
                    ```

                    示例：
                    ```sql
                    DELETE FROM student
                    WHERE id = 3;
                    ```

                    风险点：
                    ```sql
                    DELETE FROM student;
                    ```
                    这会删除表中全部记录（表结构仍保留）。

                    ## 八、三者对比
                    | 语句 | 作用 | 是否需要 WHERE | 主要风险 |
                    | --- | --- | --- | --- |
                    | INSERT | 新增数据 | 不需要 | 字段和值不匹配、主键冲突 |
                    | UPDATE | 修改数据 | 通常需要 | 漏写 WHERE 导致整表更新 |
                    | DELETE | 删除数据 | 通常需要 | 漏写 WHERE 导致整表删除 |

                    ## 九、常见误区
                    - 把 UPDATE/DELETE 当成“默认只改一条”，实际上满足条件的都会被影响
                    - 不先查询就直接改删
                    - 使用非唯一条件更新，造成误改

                    ## 十、最佳实践
                    1. 先查后改、先查后删  
                    2. 优先使用主键或唯一键定位  
                    3. 关键变更放在事务中执行  
                    4. 重要环境先备份，再执行批量改删

                    ## 十一、小结
                    INSERT、UPDATE、DELETE 是 MySQL 数据操作的核心基础。  
                    真正掌握的标准是：能写出正确 SQL，并能在执行前评估影响范围，避免误操作。
                    """;
        }

        if (title.contains("Spring Boot") && title.contains("Spring Framework") && title.contains("关系")) {
            return """
                    # Spring Boot 与 Spring Framework 的关系

                    ## 知识点定义
                    Spring Framework 是 Java 生态中用于构建企业级应用的基础框架，核心能力包括 IoC 容器、依赖注入、AOP、事务管理、Web MVC、数据访问集成等。Spring Boot 不是 Spring Framework 的替代品，而是在 Spring Framework 之上提供的一套快速开发与自动配置体系。

                    简单说：Spring Framework 提供“能力”，Spring Boot 提供“更快、更少配置地使用这些能力”的工程化方式。

                    ## 背景与作用
                    早期使用 Spring 开发 Web 项目时，开发者通常需要手动配置大量 XML 或 Java Config，例如组件扫描、MVC 配置、数据源、事务、JSON 转换器、内嵌容器等。项目能跑起来之前，配置成本很高。

                    Spring Boot 出现的目的，就是降低 Spring 应用的启动和集成成本。它通过自动配置、起步依赖和内嵌服务器，让开发者可以更快创建一个可运行的 Spring 应用。

                    ## 核心原理或核心关系
                    Spring Boot 的底层仍然依赖 Spring Framework。比如你在 Spring Boot 项目中写：

                    ```java
                    @RestController
                    public class HelloController {
                        @GetMapping("/hello")
                        public String hello() {
                            return "hello";
                        }
                    }
                    ```

                    这里的 `@RestController`、`@GetMapping`、Bean 管理和请求分发，本质上都来自 Spring Framework 的能力。Spring Boot 做的是自动帮你配置好 Spring MVC、JSON 序列化、内嵌 Tomcat 和默认运行环境。

                    `@SpringBootApplication` 是理解二者关系的入口：

                    ```java
                    @SpringBootApplication
                    public class DemoApplication {
                        public static void main(String[] args) {
                            SpringApplication.run(DemoApplication.class, args);
                        }
                    }
                    ```

                    它组合了组件扫描、自动配置和配置声明。也就是说，Spring Boot 通过约定和自动配置把 Spring Framework 的复杂配置隐藏起来，但运行时依然是 Spring 容器在管理 Bean 和处理请求。

                    ## 关键区别 / 联系
                    | 对比项 | Spring Framework | Spring Boot |
                    | --- | --- | --- |
                    | 定位 | 基础框架 | 快速应用开发脚手架与自动配置体系 |
                    | 核心能力 | IoC、AOP、事务、MVC、数据访问 | 自动配置、起步依赖、内嵌服务器、运行监控 |
                    | 配置方式 | 更偏手动配置 | 约定优于配置，自动推断 |
                    | 是否能单独使用 | 可以 | 依赖 Spring Framework |
                    | 解决的问题 | 如何组织和管理 Java 应用对象 | 如何快速搭建和运行 Spring 应用 |

                    联系是：Spring Boot 建立在 Spring Framework 之上。区别是：Spring Framework 更像发动机，Spring Boot 更像整车工程方案，把发动机、默认配置、启动方式和常用组件装配好。

                    ## 示例或代码示例
                    如果不用 Spring Boot，你可能需要手动配置 MVC、Tomcat、Jackson、组件扫描等。使用 Spring Boot 后，只需要引入 starter：

                    ```xml
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    ```

                    这个 starter 会间接引入 Spring MVC、Jackson、Tomcat 等依赖，并触发对应自动配置。你写 Controller 后应用就能直接启动，这就是 Spring Boot 对 Spring Framework 的工程化封装。

                    ## 常见误区
                    1. 误以为 Spring Boot 替代了 Spring Framework。实际上 Boot 离不开 Framework。
                    2. 只会用 Boot 注解，却不理解 Bean、IoC、MVC 等底层 Spring 概念，后续排查问题会很困难。
                    3. 把自动配置当成“魔法”。自动配置本质上是根据 classpath、配置项和条件注解创建默认 Bean。
                    4. 认为 Boot 项目不需要配置。真实项目仍然需要配置数据库、缓存、安全、日志和环境变量。

                    ## 小结
                    Spring Framework 是能力基础，Spring Boot 是快速使用这些能力的工程化方案。学习 Spring Boot 时不能只记启动注解，还要逐步理解它背后调用的 Spring 容器、Bean 管理、自动配置和 Web MVC 机制。
                    """;
        }
        String topic = nullToBlank(direction.getName());
        String planTitle = planContext == null ? topic : nullToBlank(planContext.title());
        String level = profile == null ? nullToBlank(pointContext.level()) : nullToBlank(profile.getCurrentLevel());
        String point = pointContext.title();
        String outcome = nullToBlank(pointContext.outcome());
        return """
                # %s

                ## 一、学习目标
                学完本节后，你应该能够：
                - 说明「%s」的定义、作用和边界
                - 在 %s 场景中正确使用该知识点
                - 识别常见误用，并给出修正方式

                ## 二、前置知识
                建议先具备以下基础再学习本节：
                - 与「%s」相邻的核心概念
                - 基础查询/调用/配置能力
                - 基本错误排查思路

                ## 三、核心讲解
                「%s」在 %s 中的价值是：把抽象概念转成可执行操作。  
                你可以从三个角度理解：
                1. 它解决什么问题（业务价值）
                2. 它如何工作（执行机制）
                3. 什么时候使用（适用条件）

                ## 四、关键区别与联系
                关注「%s」与相邻概念的差异：
                - 联系：都服务于同一条业务链路
                - 区别：职责范围、触发条件、结果形态不同
                - 适用场景：优先选择能最小化副作用的实现方式

                ## 五、示例（可执行思路）
                > 学习计划：%s  
                > 当前水平：%s  
                > 知识点目标：%s

                示例流程：
                1. 先写一个最小可运行示例，聚焦单一能力
                2. 再补一个边界场景，验证异常输入或极端条件
                3. 最后增加一个“错误示例 + 修复示例”，建立判断标准

                ## 六、常见误区
                - 只记结论，不看触发条件
                - 看懂示例，但不会独立改写
                - 忽略边界条件，导致线上行为不稳定

                ## 七、小结
                真正掌握「%s」的标准不是背定义，而是能独立完成：  
                **正确实现 -> 自检验证 -> 问题修复** 这条闭环。
                """.formatted(point, point, topic, point, point, topic, point, planTitle, level, outcome, point);
    }

    private GeneratedAnswer fallbackAnswer(String question, String retrievedContext) {
        if (retrievedContext == null || retrievedContext.isBlank()) {
            return new GeneratedAnswer(
                    "当前回答处于无知识库模式：我没有引用你的计划资料，而是基于通用知识来讲解。\n\n"
                            + "针对你的问题「" + question + "」，可以先按三步理解：第一，明确核心概念和适用场景；第二，把概念拆成可观察的职责、输入和输出；第三，用一个小练习验证自己是否能独立应用。\n\n"
                            + "如果你后续上传资料，我会优先结合资料内容给出更贴合当前计划的讲解和引用来源。",
                    "[\"无知识库模式\", \"通用讲解\", \"学习拆解\"]",
                    "[\"给我举一个具体例子\", \"帮我生成一道练习题\", \"用更简单的话解释\"]"
            );
        }

        String clipped = retrievedContext.length() > 700 ? retrievedContext.substring(0, 700) + "..." : retrievedContext;
        String answer = "基于当前计划知识库，和你的问题「" + question + "」最相关的内容是：\n\n"
                + clipped
                + "\n\n可以先把它拆成三步理解：先明确概念边界，再看它在当前学习任务中的职责，最后用一个小练习验证是否真的掌握。";
        return new GeneratedAnswer(
                answer,
                "[\"计划级知识库\", \"资料引用\", \"概念拆解\"]",
                "[\"能举一个例子吗？\", \"帮我生成一道练习题\", \"讲得更简单一点\"]"
        );
    }

    private SummaryCardDraft fallbackSummaryCard(Long planId, String planTitle, Long documentId, String documentName, String documentContent) {
        String text = documentContent == null ? "" : documentContent.replace("\n", " ").trim();
        String summary = text.isBlank()
                ? "这份总结提炼了文档中的核心知识、关键词和复习提醒。"
                : (text.length() > 80 ? text.substring(0, 80) + "..." : text);
        List<String> highlights = List.of(
                "先把文档中的核心定义和适用场景梳理清楚",
                "重点关注文档反复出现的概念、步骤和判断条件",
                "把容易混淆的知识点单独记录，后续结合题目复习",
                "学习后用问答或测验验证是否真正掌握"
        );
        List<String> keywords = List.of(
                planTitle == null || planTitle.isBlank() ? "学习计划" : planTitle,
                documentName == null || documentName.isBlank() ? "来源文档" : documentName.replaceAll("\\.[^.]+$", ""),
                "知识总结",
                "复习重点"
        );
        return new SummaryCardDraft(
                (documentName == null || documentName.isBlank() ? "文档" : documentName.replaceAll("\\.[^.]+$", "")) + " 知识总结",
                summary,
                highlights,
                keywords,
                List.of("不要只看结论，需结合例子或题目验证理解", "遇到相近概念时，优先比较边界和使用条件"),
                String.valueOf(documentId),
                documentName,
                String.valueOf(planId)
        );
    }

    private String pickAnswer(GenerateProfileRequest request, int index, String fallback) {
        if (request.answers().size() <= index) {
            return fallback;
        }
        String answer = request.answers().get(index).answer();
        return answer == null || answer.isBlank() ? fallback : answer;
    }

    private String inferLevel(String basis) {
        String text = basis == null ? "" : basis;
        if (text.contains("零") || text.contains("没有") || text.contains("新手")) {
            return "BEGINNER";
        }
        if (text.contains("项目") || text.contains("工作") || text.contains("熟悉")) {
            return "INTERMEDIATE";
        }
        return "FOUNDATION";
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private String textOrDefault(JsonNode root, String field, String fallback) {
        String value = text(root, field);
        return value.isBlank() ? fallback : value;
    }

    private List<String> stringList(JsonNode node, int limit) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    values.add(value.length() > 90 ? value.substring(0, 90) : value);
                }
                if (values.size() >= limit) {
                    break;
                }
            }
        }
        return values;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    public record LearningPlanContext(String title, String goal) {
    }

    public record KnowledgePointContext(
            String id,
            String title,
            String chapterName,
            String unitName,
            String level,
            String outcome
    ) {
    }

    public record SummaryCardDraft(
            String title,
            String summary,
            List<String> highlights,
            List<String> keywords,
            List<String> tips,
            String sourceDocumentId,
            String sourceDocumentName,
            String planId
    ) {
    }
}
