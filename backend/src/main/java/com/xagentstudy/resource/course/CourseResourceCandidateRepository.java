package com.xagentstudy.resource.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseResourceCandidateRepository extends JpaRepository<CourseResourceCandidate, Long> {
    List<CourseResourceCandidate> findAllByOrderByCreatedAtDesc();

    List<CourseResourceCandidate> findAllByPublishedResourceId(Long publishedResourceId);
}
