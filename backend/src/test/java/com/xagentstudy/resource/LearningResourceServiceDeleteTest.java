package com.xagentstudy.resource;

import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.resource.course.CourseCrawlerCandidate;
import com.xagentstudy.resource.course.CourseResourceCandidate;
import com.xagentstudy.resource.course.CourseResourceCandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningResourceServiceDeleteTest {
    @Test
    void detachesPublishedCourseCandidatesBeforeDeletingResource() {
        LearningResourceRepository resourceRepository = mock(LearningResourceRepository.class);
        LearningPlanRepository planRepository = mock(LearningPlanRepository.class);
        LearningDirectionRepository directionRepository = mock(LearningDirectionRepository.class);
        CourseResourceCandidateRepository candidateRepository = mock(CourseResourceCandidateRepository.class);
        LearningResourceService service = new LearningResourceService(
                resourceRepository,
                planRepository,
                directionRepository,
                candidateRepository,
                "uploads"
        );
        LearningResource resource = new LearningResource(
                7L,
                "Redis 课程",
                "Bilibili 公开视频",
                "EXTERNAL_COURSE",
                "https://www.bilibili.com/video/BV1cr4y1671t",
                "text/html",
                "https://www.bilibili.com/video/BV1cr4y1671t",
                "Redis",
                "后端开发",
                "Redis,缓存",
                0L
        );
        ReflectionTestUtils.setField(resource, "id", 5L);
        CourseResourceCandidate candidate = new CourseResourceCandidate(new CourseCrawlerCandidate(
                "Redis 入门课程",
                "公开课程",
                "https://www.bilibili.com/video/BV1cr4y1671t",
                "Bilibili",
                "https://example.com/cover.jpg",
                "Redis",
                "后端开发",
                List.of("Redis", "缓存"),
                "入门"
        ), 7L);
        candidate.publish(5L);

        when(resourceRepository.findById(5L)).thenReturn(Optional.of(resource));
        when(candidateRepository.findAllByPublishedResourceId(5L)).thenReturn(List.of(candidate));

        service.delete(5L);

        assertThat(candidate.getStatus()).isEqualTo("PENDING");
        assertThat(candidate.getPublishedResourceId()).isNull();
        verify(candidateRepository).findAllByPublishedResourceId(5L);
        verify(resourceRepository).delete(resource);
    }
}
