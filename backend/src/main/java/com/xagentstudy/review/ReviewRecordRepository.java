package com.xagentstudy.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    List<ReviewRecord> findByPlanIdAndUserIdOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(Long planId, Long userId);

    List<ReviewRecord> findByUserIdAndPlanIdInOrderByRecommendedAtDesc(Long userId, List<Long> planIds);

    Optional<ReviewRecord> findFirstByPlanIdAndUserIdAndKnowledgePointOrderByCompletedAscPriorityLevelDescRecommendedAtDesc(Long planId, Long userId, String knowledgePoint);

    long countByPlanIdInAndUserIdAndCompletedFalse(List<Long> planIds, Long userId);

    void deleteByPlanId(Long planId);
}
