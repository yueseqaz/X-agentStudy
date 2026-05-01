package com.xagentstudy.resourcecollab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionTaskRepository extends JpaRepository<IngestionTask, Long> {
    List<IngestionTask> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
}
