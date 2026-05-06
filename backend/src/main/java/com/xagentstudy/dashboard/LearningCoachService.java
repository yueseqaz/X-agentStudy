package com.xagentstudy.dashboard;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningCoachService {
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final QuestionRepository questionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final PlanTaskRecordRepository taskRecordRepository;

    public LearningCoachService(
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            KnowledgeDocumentRepository documentRepository,
            QuestionRepository questionRepository,
            ReviewRecordRepository reviewRecordRepository,
            PlanTaskRecordRepository taskRecordRepository
    ) {
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.documentRepository = documentRepository;
        this.questionRepository = questionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.taskRecordRepository = taskRecordRepository;
    }

    @Transactional(readOnly = true)
    public LearningCoachResponse today() {
        try {
            return buildToday();
        } catch (RuntimeException ex) {
            return emptyPlanAdvice();
        }
    }

    private LearningCoachResponse buildToday() {
        Long userId = AuthContext.currentUserId();
        List<LearningDirection> directions = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId);
        if (directions.isEmpty()) {
            return emptyPlanAdvice();
        }

        Map<Long, LearningDirection> directionMap = directions.stream()
                .collect(Collectors.toMap(LearningDirection::getId, Function.identity()));
        List<Long> directionIds = directions.stream().map(LearningDirection::getId).toList();
        List<LearningPlan> plans = planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds);
        if (plans.isEmpty()) {
            return emptyPlanAdvice();
        }

        LearningPlan plan = plans.get(0);
        LearningDirection direction = directionMap.get(plan.getDirectionId());
        List<Long> planIds = plans.stream().map(LearningPlan::getId).toList();
        long documentCount = documentRepository.countByPlanIdIn(List.of(plan.getId()));
        long questionCount = questionRepository.countByPlanIdIn(List.of(plan.getId()));
        long pendingReviewCount = reviewRecordRepository.countByPlanIdInAndUserIdAndCompletedFalse(planIds, userId);
        long totalTasks = taskRecordRepository.countByPlanIdAndUserId(plan.getId(), userId);
        long completedTasks = taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrue(plan.getId(), userId);

        if (documentCount == 0) {
            return planAdvice(
                    plan,
                    direction,
                    "先给计划补一份核心资料",
                    "当前计划还没有知识库资料，今天先补足学习上下文，后续答疑、测验和报告才会更准。",
                    25,
                    "去补资料",
                    "/plans/" + plan.getId() + "/knowledge",
                    List.of("上传一份最核心的教材、文档或笔记", "等待资料解析完成", "用资料生成第一组测验题"),
                    signals(documentCount, questionCount, pendingReviewCount, completedTasks, totalTasks)
            );
        }
        if (questionCount == 0) {
            return planAdvice(
                    plan,
                    direction,
                    "今天先生成一组检验题",
                    "知识库已有资料，但还没有题目验证掌握情况。先做小测，才能知道后面该补哪里。",
                    20,
                    "去生成测验",
                    "/plans/" + plan.getId() + "?tab=quiz",
                    List.of("基于资料生成 5 道题", "完成第一轮作答", "查看错题和薄弱点"),
                    signals(documentCount, questionCount, pendingReviewCount, completedTasks, totalTasks)
            );
        }
        if (pendingReviewCount > 0) {
            return planAdvice(
                    plan,
                    direction,
                    "先清掉今天的待复习",
                    "你已经有待复习知识点。先处理复习，再推进新内容，学习会更稳。",
                    18,
                    "去复习",
                    "/plans/" + plan.getId() + "?tab=review",
                    List.of("完成待复习条目", "重做相关错题", "刷新学习报告看薄弱点变化"),
                    signals(documentCount, questionCount, pendingReviewCount, completedTasks, totalTasks)
            );
        }
        if (totalTasks > 0 && completedTasks < totalTasks) {
            return planAdvice(
                    plan,
                    direction,
                    "推进一个当前阶段任务",
                    "资料、题目和复习状态都可用，今天适合继续推进计划任务，并保持节奏。",
                    30,
                    "继续学习",
                    "/plans/" + plan.getId(),
                    List.of("完成一个未完成任务", "针对不懂处提一个问题", "学习结束后打卡记录"),
                    signals(documentCount, questionCount, pendingReviewCount, completedTasks, totalTasks)
            );
        }
        return planAdvice(
                plan,
                direction,
                "做一次阶段复盘",
                "当前计划任务完成度较高，今天适合整理成果、刷新报告，再决定下一阶段。",
                15,
                "查看报告",
                "/plans/" + plan.getId() + "?tab=report",
                List.of("查看学习报告", "生成一张总结卡片", "按报告建议调整下一步"),
                signals(documentCount, questionCount, pendingReviewCount, completedTasks, totalTasks)
        );
    }

    private LearningCoachResponse emptyPlanAdvice() {
        return new LearningCoachResponse(
                null,
                null,
                null,
                "先定一个清晰学习方向",
                "还没有可执行的学习计划。先把目标、基础和周期说清楚，系统才能带你往前走。",
                12,
                "创建学习方向",
                "/directions",
                List.of("创建一个具体学习方向", "完成画像问答", "生成第一份学习计划"),
                List.of("暂无学习计划", "暂无知识库资料", "暂无测验题")
        );
    }

    private LearningCoachResponse planAdvice(
            LearningPlan plan,
            LearningDirection direction,
            String headline,
            String reason,
            int estimatedMinutes,
            String primaryActionLabel,
            String primaryActionPath,
            List<String> nextSteps,
            List<String> signals
    ) {
        return new LearningCoachResponse(
                plan.getId(),
                plan.getTitle(),
                direction == null ? "" : direction.getName(),
                headline,
                reason,
                estimatedMinutes,
                primaryActionLabel,
                primaryActionPath,
                nextSteps,
                signals
        );
    }

    private List<String> signals(long documentCount, long questionCount, long pendingReviewCount, long completedTasks, long totalTasks) {
        return List.of(
                "资料 " + documentCount + " 份",
                "题目 " + questionCount + " 道",
                "待复习 " + pendingReviewCount + " 项",
                "任务完成 " + completedTasks + "/" + totalTasks
        );
    }
}
