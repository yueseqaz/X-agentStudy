package com.xagentstudy.resource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTagMatcherTest {
    @Test
    void matchesDirectionNameAgainstSubjectAndTags() {
        LearningResource resource = new LearningResource(
                1L,
                "Vue 组件设计",
                "视频课程",
                "VIDEO",
                "vue.mp4",
                "video/mp4",
                "/tmp/vue.mp4",
                "Vue",
                "前端开发",
                "组件,Composition API",
                1024L
        );

        assertThat(ResourceTagMatcher.matches("Vue 前端开发", "编程开发", resource)).isTrue();
    }

    @Test
    void ignoresUnrelatedResources() {
        LearningResource resource = new LearningResource(
                1L,
                "线性代数入门",
                "文档",
                "DOCUMENT",
                "math.pdf",
                "application/pdf",
                "/tmp/math.pdf",
                "数学",
                "基础学科",
                "矩阵,向量",
                1024L
        );

        assertThat(ResourceTagMatcher.matches("Java 后端开发", "编程开发", resource)).isFalse();
    }
}
