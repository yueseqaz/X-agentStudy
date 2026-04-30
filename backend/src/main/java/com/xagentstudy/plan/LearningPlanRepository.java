package com.xagentstudy.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {
    Optional<LearningPlan> findFirstByDirectionIdOrderByCreatedAtDesc(Long directionId);

    List<LearningPlan> findByDirectionIdOrderByCreatedAtDesc(Long directionId);

    List<LearningPlan> findByDirectionIdInOrderByCreatedAtDesc(List<Long> directionIds);
}
