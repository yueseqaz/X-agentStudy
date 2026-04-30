package com.xagentstudy.qa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface QARecordRepository extends JpaRepository<QARecord, Long> {
    List<QARecord> findByPlanIdOrderByCreatedAtDesc(Long planId);

    long countByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime start, OffsetDateTime end);

    void deleteByPlanId(Long planId);
}
