package com.xagentstudy.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningProfileRepository extends JpaRepository<LearningProfile, Long> {
    Optional<LearningProfile> findFirstByDirectionIdOrderByCreatedAtDesc(Long directionId);

    void deleteByDirectionId(Long directionId);
}
