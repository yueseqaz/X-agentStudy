package com.xagentstudy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPlanIdOrderByCreatedAtDesc(Long planId);

    List<Question> findByPlanIdAndSourceScopeOrderByCreatedAtDesc(Long planId, String sourceScope);

    List<Question> findByPlanIdAndSourceScopeAndQuizBatchIdOrderByCreatedAtDesc(Long planId, String sourceScope, String quizBatchId);

    long countByPlanIdIn(List<Long> planIds);

    void deleteByPlanId(Long planId);
}
