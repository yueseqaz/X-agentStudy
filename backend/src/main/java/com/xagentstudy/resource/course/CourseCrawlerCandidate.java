package com.xagentstudy.resource.course;

import java.util.List;

public record CourseCrawlerCandidate(
        String title,
        String description,
        String url,
        String source,
        String coverUrl,
        String subjectName,
        String subjectScope,
        List<String> tags,
        String difficulty
) {
}
