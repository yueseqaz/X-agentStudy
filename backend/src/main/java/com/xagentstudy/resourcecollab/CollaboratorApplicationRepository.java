package com.xagentstudy.resourcecollab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaboratorApplicationRepository extends JpaRepository<CollaboratorApplication, Long> {
    Optional<CollaboratorApplication> findByUserId(Long userId);

    List<CollaboratorApplication> findAllByOrderByCreatedAtDesc();
}
