package com.xagentstudy.resource.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseResourceCandidateTest {
    @Test
    void detachesPublishedResourceWhenResourceIsDeleted() {
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

        candidate.detachPublishedResource();

        assertThat(candidate.getStatus()).isEqualTo("PENDING");
        assertThat(candidate.getPublishedResourceId()).isNull();
    }
}
