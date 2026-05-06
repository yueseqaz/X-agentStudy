package com.xagentstudy.review;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.report.LearningReportResponse;
import com.xagentstudy.report.LearningReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningReportService reportService;
    private final ReviewRecordRepository reviewRecordRepository;

    public ReviewService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            LearningReportService reportService,
            ReviewRecordRepository reviewRecordRepository
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.reportService = reportService;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    @Transactional
    public ReviewSummaryResponse today(Long planId) {
        ensurePlan(planId);
        LearningReportResponse report = reportService.getReport(planId);
        int priority = Math.max(1, report.weakPoints().size());
        for (String point : report.weakPoints()) {
            int currentPriority = priority--;
            try {
                String safePoint = point == null ? "未命名知识点" : point.trim();
                if (safePoint.length() > 120) {
                    safePoint = safePoint.substring(0, 120);
                }
                String finalSafePoint = safePoint;
                reviewRecordRepository.findFirstByPlanIdAndUserIdAndKnowledgePointOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(planId, AuthContext.currentUserId(), finalSafePoint)
                        .orElseGet(() -> reviewRecordRepository.save(new ReviewRecord(
                                planId,
                                AuthContext.currentUserId(),
                                finalSafePoint,
                                "来自错题聚合的薄弱知识点，建议结合解析重做相关题目。",
                                currentPriority
                        )));
            } catch (Exception ex) {
                log.warn("Failed to sync review record for plan {} and point {}", planId, point, ex);
            }
        }
        return summary(planId);
    }

    @Transactional
    public ReviewRecordResponse complete(Long planId, Long reviewId, CompleteReviewRequest request) {
        ensurePlan(planId);
        ReviewRecord record = reviewRecordRepository.findById(reviewId)
                .filter(item -> item.getPlanId().equals(planId) && item.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Review record not found"));
        record.complete(request == null ? 4 : request.safeQuality());
        return ReviewRecordResponse.from(record);
    }

    private ReviewSummaryResponse summary(Long planId) {
        List<ReviewRecordResponse> records = reviewRecordRepository
                .findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(planId, AuthContext.currentUserId())
                .stream()
                .map(ReviewRecordResponse::from)
                .toList();
        int completedCount = (int) records.stream().filter(ReviewRecordResponse::completed).count();
        return new ReviewSummaryResponse(
                planId,
                records.size(),
                records.size() - completedCount,
                completedCount,
                records
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
}
