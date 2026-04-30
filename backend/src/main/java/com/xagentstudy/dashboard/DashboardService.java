package com.xagentstudy.dashboard;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.quiz.QuestionRepository;
import com.xagentstudy.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final QuestionRepository questionRepository;
    private final ReviewRecordRepository reviewRecordRepository;

    public DashboardService(
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            KnowledgeDocumentRepository documentRepository,
            QuestionRepository questionRepository,
            ReviewRecordRepository reviewRecordRepository
    ) {
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.documentRepository = documentRepository;
        this.questionRepository = questionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        List<Long> directionIds = directionRepository
                .findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(AuthContext.currentUserId())
                .stream()
                .map(LearningDirection::getId)
                .toList();
        if (directionIds.isEmpty()) {
            return new DashboardSummaryResponse(0, 0, 0, 0);
        }
        List<Long> planIds = planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds)
                .stream()
                .map(LearningPlan::getId)
                .toList();
        if (planIds.isEmpty()) {
            return new DashboardSummaryResponse(0, 0, 0, 0);
        }
        return new DashboardSummaryResponse(
                planIds.size(),
                documentRepository.countByPlanIdIn(planIds),
                questionRepository.countByPlanIdIn(planIds),
                reviewRecordRepository.countByPlanIdInAndUserIdAndCompletedFalse(planIds, AuthContext.currentUserId())
        );
    }
}
