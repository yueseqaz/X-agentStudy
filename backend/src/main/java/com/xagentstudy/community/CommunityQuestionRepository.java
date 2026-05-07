package com.xagentstudy.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, Long> {
    List<CommunityQuestion> findAllByOrderByUpdatedAtDesc();
}
