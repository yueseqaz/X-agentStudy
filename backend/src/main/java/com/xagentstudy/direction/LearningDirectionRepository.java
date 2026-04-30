package com.xagentstudy.direction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningDirectionRepository extends JpaRepository<LearningDirection, Long> {
    List<LearningDirection> findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(Long userId);
}
