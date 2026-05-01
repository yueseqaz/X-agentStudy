package com.xagentstudy.summary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SummaryCardRepository extends JpaRepository<SummaryCard, Long> {
    List<SummaryCard> findByPlanIdAndUserIdOrderByCreatedAtDesc(Long planId, Long userId);

    List<SummaryCard> findTop5ByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, OffsetDateTime start, OffsetDateTime end);

    void deleteByPlanId(Long planId);

    void deleteByDocumentId(Long documentId);
}
