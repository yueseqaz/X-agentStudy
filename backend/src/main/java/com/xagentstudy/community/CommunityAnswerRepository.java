package com.xagentstudy.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, Long> {
    List<CommunityAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    long countByQuestionId(Long questionId);
}
