package com.xagentstudy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    List<AnswerRecord> findByQuestionIdOrderByAnsweredAtDesc(Long questionId);

    List<AnswerRecord> findByQuestionIdInAndUserIdOrderByAnsweredAtDesc(List<Long> questionIds, Long userId);

    long countByUserIdAndAnsweredAtBetween(Long userId, OffsetDateTime start, OffsetDateTime end);

    long countByUserIdAndCorrectTrueAndAnsweredAtBetween(Long userId, OffsetDateTime start, OffsetDateTime end);

    void deleteByQuestionIdIn(List<Long> questionIds);
}
