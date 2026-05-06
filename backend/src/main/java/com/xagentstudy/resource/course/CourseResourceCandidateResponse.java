package com.xagentstudy.resource.course;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public record CourseResourceCandidateResponse(
        Long id,
        String title,
        String description,
        String courseUrl,
        String sourceName,
        String coverUrl,
        String subjectName,
        String subjectScope,
        String tags,
        List<String> tagList,
        String difficulty,
        String status,
        Long publishedResourceId,
        OffsetDateTime createdAt
) {
    public static CourseResourceCandidateResponse from(CourseResourceCandidate candidate) {
        return new CourseResourceCandidateResponse(
                candidate.getId(),
                candidate.getTitle(),
                candidate.getDescription(),
                candidate.getCourseUrl(),
                candidate.getSourceName(),
                candidate.getCoverUrl(),
                candidate.getSubjectName(),
                candidate.getSubjectScope(),
                candidate.getTags(),
                Arrays.stream((candidate.getTags() == null ? "" : candidate.getTags()).split("[,，]"))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList(),
                candidate.getDifficulty(),
                candidate.getStatus(),
                candidate.getPublishedResourceId(),
                candidate.getCreatedAt()
        );
    }
}
