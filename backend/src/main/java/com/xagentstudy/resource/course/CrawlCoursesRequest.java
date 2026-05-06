package com.xagentstudy.resource.course;

public record CrawlCoursesRequest(
        String query,
        String url,
        String subjectName,
        String subjectScope,
        String tags,
        Integer limit
) {
}
