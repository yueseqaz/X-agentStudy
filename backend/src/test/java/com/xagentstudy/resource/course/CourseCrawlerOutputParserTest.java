package com.xagentstudy.resource.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseCrawlerOutputParserTest {
    private final CourseCrawlerOutputParser parser = new CourseCrawlerOutputParser();

    @Test
    void parsesCrawlerJsonIntoCourseCandidates() {
        String json = """
                [
                  {
                    "title": "Redis 入门课程",
                    "description": "公开课程，讲解 Redis 基础命令",
                    "url": "https://example.com/redis",
                    "source": "Example",
                    "coverUrl": "https://example.com/redis.png",
                    "subjectName": "Redis",
                    "subjectScope": "后端开发",
                    "tags": ["Redis", "缓存", "数据库"],
                    "difficulty": "入门"
                  }
                ]
                """;

        List<CourseCrawlerCandidate> candidates = parser.parse(json);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).title()).isEqualTo("Redis 入门课程");
        assertThat(candidates.get(0).url()).isEqualTo("https://example.com/redis");
        assertThat(candidates.get(0).tags()).containsExactly("Redis", "缓存", "数据库");
    }
}
