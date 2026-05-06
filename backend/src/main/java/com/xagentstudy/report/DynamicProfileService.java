package com.xagentstudy.report;

import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.profile.LearningProfile;
import com.xagentstudy.profile.LearningProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicProfileService {
    private final LearningPlanRepository planRepository;
    private final LearningProfileRepository profileRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final LearningReportService reportService;

    public DynamicProfileService(
            LearningPlanRepository planRepository,
            LearningProfileRepository profileRepository,
            KnowledgeDocumentRepository documentRepository,
            LearningReportService reportService
    ) {
        this.planRepository = planRepository;
        this.profileRepository = profileRepository;
        this.documentRepository = documentRepository;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public DynamicProfileResponse getDynamicProfile(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        LearningProfile profile = profileRepository.findFirstByDirectionIdOrderByCreatedAtDesc(plan.getDirectionId()).orElse(null);
        LearningReportResponse report = reportService.getReport(planId);
        List<KnowledgeDocument> documents = documentRepository.findByPlanIdOrderByUploadedAtDesc(planId);
        List<DynamicProfileDimensionResponse> dimensions = dimensions(profile, report, documents);
        int completenessScore = Math.round((float) dimensions.stream().mapToInt(DynamicProfileDimensionResponse::confidence).average().orElse(0));
        return new DynamicProfileResponse(
                planId,
                completenessScore,
                summary(profile, report, completenessScore),
                dimensions,
                updateSignals(report, documents),
                recommendationReasons(report, documents)
        );
    }

    private List<DynamicProfileDimensionResponse> dimensions(
            LearningProfile profile,
            LearningReportResponse report,
            List<KnowledgeDocument> documents
    ) {
        List<DynamicProfileDimensionResponse> items = new ArrayList<>();
        items.add(dimension(
                "知识基础",
                profile == null ? "待补充" : blankToDefault(profile.getCurrentLevel(), "未说明基础"),
                profile == null ? 0 : 90,
                List.of(profile == null ? "还没有学习画像" : "来自画像问答")
        ));
        items.add(dimension(
                "学习目标",
                profile == null ? "待补充" : blankToDefault(profile.getGoal(), "未说明目标"),
                profile == null ? 0 : 92,
                List.of(profile == null ? "需要先完成画像问答" : "来自画像目标与当前计划")
        ));
        items.add(dimension(
                "学习风格",
                profile == null ? "待观察" : blankToDefault(profile.getPreference(), "偏好尚不明确"),
                profile == null ? 20 : 86,
                List.of(profile == null ? "暂未形成偏好" : "来自画像偏好", documents.isEmpty() ? "暂无资料学习行为" : "已有 " + documents.size() + " 份资料行为")
        ));
        items.add(dimension(
                "薄弱点",
                report.weakPoints().isEmpty() ? "暂无明显薄弱点" : String.join("、", report.weakPoints().subList(0, Math.min(3, report.weakPoints().size()))),
                report.answeredQuestionCount() == 0 ? 35 : 88,
                List.of("来自错题和学习报告", "错题 " + report.wrongQuestionCount() + " 道")
        ));
        items.add(dimension(
                "学习节奏",
                profile == null ? "待补充" : blankToDefault(profile.getTimeBudget(), "未说明学习时间"),
                profile == null ? 0 : Math.max(60, (int) Math.round(report.taskCompletionRate())),
                List.of("任务完成率 " + report.taskCompletionRate() + "%", profile == null ? "暂无时间预算" : "来自画像时间预算")
        ));
        items.add(dimension(
                "资源偏好",
                resourcePreference(profile, documents),
                documents.isEmpty() ? 45 : 82,
                List.of(documents.isEmpty() ? "暂无知识库资料" : "最近资料：" + documents.get(0).getName())
        ));
        items.add(dimension(
                "易错类型",
                report.weaknessReasons().isEmpty() ? "待通过测验发现" : report.weaknessReasons().get(0),
                report.wrongQuestionCount() == 0 ? 30 : 84,
                List.of("来自错题原因聚合", "已作答 " + report.answeredQuestionCount() + " 道")
        ));
        items.add(dimension(
                "推荐可信度",
                report.questionCount() == 0 ? "需要更多答题数据" : "可基于答题、资料和复习生成推荐",
                Math.min(95, Math.max(35, report.masteryScore() + (documents.isEmpty() ? 0 : 15))),
                List.of("掌握度 " + report.masteryScore(), "资料 " + documents.size() + " 份")
        ));
        return items;
    }

    private DynamicProfileDimensionResponse dimension(String name, String value, int confidence, List<String> evidence) {
        String status = confidence >= 80 ? "稳定" : confidence >= 50 ? "观察中" : "待补充";
        return new DynamicProfileDimensionResponse(name, value, Math.max(0, Math.min(100, confidence)), status, evidence);
    }

    private List<DynamicProfileSignalResponse> updateSignals(LearningReportResponse report, List<KnowledgeDocument> documents) {
        List<DynamicProfileSignalResponse> signals = new ArrayList<>();
        if (!report.weakPoints().isEmpty()) {
            signals.add(new DynamicProfileSignalResponse("错题表现", "薄弱点更新为：" + String.join("、", report.weakPoints().subList(0, Math.min(3, report.weakPoints().size()))), "高"));
        }
        if (report.taskCompletionRate() < 70) {
            signals.add(new DynamicProfileSignalResponse("任务进度", "任务完成率偏低，学习节奏需要放慢或拆小。", "中"));
        }
        if (!documents.isEmpty()) {
            signals.add(new DynamicProfileSignalResponse("资料行为", "最近学习资料：" + documents.get(0).getName(), "中"));
        }
        if (signals.isEmpty()) {
            signals.add(new DynamicProfileSignalResponse("系统观察", "暂无足够行为数据，建议先完成一次测验或复习。", "低"));
        }
        return signals;
    }

    private List<DynamicProfileRecommendationResponse> recommendationReasons(LearningReportResponse report, List<KnowledgeDocument> documents) {
        List<DynamicProfileRecommendationResponse> reasons = new ArrayList<>();
        if (!report.weakPoints().isEmpty()) {
            reasons.add(new DynamicProfileRecommendationResponse("题库与复习", "根据薄弱点推荐针对练习。", "当前薄弱点：" + String.join("、", report.weakPoints().subList(0, Math.min(3, report.weakPoints().size())))));
        }
        if (documents.isEmpty()) {
            reasons.add(new DynamicProfileRecommendationResponse("知识库", "资料不足会降低个性化精度。", "建议先生成或上传一份核心讲义。"));
        } else {
            reasons.add(new DynamicProfileRecommendationResponse("视频与讲义", "可从资料内容生成讲解文档、导图和视频。", "已读取 " + documents.size() + " 份资料。"));
        }
        if (report.masteryScore() < 70) {
            reasons.add(new DynamicProfileRecommendationResponse("学习路径", "掌握度偏低时优先补基础，不建议直接进入高阶内容。", "当前掌握度 " + report.masteryScore()));
        }
        return reasons;
    }

    private String summary(LearningProfile profile, LearningReportResponse report, int completenessScore) {
        if (profile == null) {
            return "画像尚未完整生成，系统会先使用计划目标和学习行为做保守推荐。";
        }
        if (!report.weakPoints().isEmpty()) {
            return "画像完整度 " + completenessScore + "%，当前最影响学习推荐的是：" + String.join("、", report.weakPoints().subList(0, Math.min(2, report.weakPoints().size()))) + "。";
        }
        return "画像完整度 " + completenessScore + "%，当前画像可支撑路径、资源、题库和视频的个性化生成。";
    }

    private String resourcePreference(LearningProfile profile, List<KnowledgeDocument> documents) {
        if (profile != null && profile.getPreference() != null && !profile.getPreference().isBlank()) {
            return profile.getPreference();
        }
        if (!documents.isEmpty()) {
            return "偏好文档资料和知识库学习";
        }
        return "待通过资料、视频和题库行为判断";
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
