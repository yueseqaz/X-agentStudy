package com.xagentstudy.resourcecollab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionSourceRepository extends JpaRepository<IngestionSource, Long> {
    List<IngestionSource> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);

    List<IngestionSource> findAllByOrderByUpdatedAtDesc();
}
