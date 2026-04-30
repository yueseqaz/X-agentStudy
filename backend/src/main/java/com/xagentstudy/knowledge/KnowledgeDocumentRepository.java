package com.xagentstudy.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByPlanIdOrderByUploadedAtDesc(Long planId);

    List<KnowledgeDocument> findByPlanIdIn(List<Long> planIds);

    long countByPlanIdIn(List<Long> planIds);

    long countByPlanIdInAndUploadedAtBetween(List<Long> planIds, OffsetDateTime start, OffsetDateTime end);

    long countByPlanIdInAndNameStartingWithAndUploadedAtBetween(List<Long> planIds, String namePrefix, OffsetDateTime start, OffsetDateTime end);
}
