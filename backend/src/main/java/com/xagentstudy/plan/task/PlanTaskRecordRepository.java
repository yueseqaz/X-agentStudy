package com.xagentstudy.plan.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanTaskRecordRepository extends JpaRepository<PlanTaskRecord, Long> {
    List<PlanTaskRecord> findByPlanIdAndUserIdOrderByStageIndexAscTaskIndexAsc(Long planId, Long userId);

    Optional<PlanTaskRecord> findByPlanIdAndUserIdAndStageIndexAndTaskIndex(Long planId, Long userId, Integer stageIndex, Integer taskIndex);

    long countByPlanIdAndUserId(Long planId, Long userId);

    long countByPlanIdAndUserIdAndCompletedTrue(Long planId, Long userId);

    long countByUserIdAndCompletedTrueAndCompletedAtBetween(Long userId, OffsetDateTime start, OffsetDateTime end);

    List<PlanTaskRecord> findByUserIdAndPlanIdInOrderByPlanIdAscStageIndexAscTaskIndexAsc(Long userId, List<Long> planIds);

    void deleteByPlanIdAndUserId(Long planId, Long userId);

    void deleteByPlanId(Long planId);
}
