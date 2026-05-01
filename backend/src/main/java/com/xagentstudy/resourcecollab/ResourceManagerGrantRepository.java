package com.xagentstudy.resourcecollab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceManagerGrantRepository extends JpaRepository<ResourceManagerGrant, Long> {
    Optional<ResourceManagerGrant> findByUserIdAndActiveTrue(Long userId);
}
