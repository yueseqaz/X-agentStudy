package com.xagentstudy.resource;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public record LearningResourceResponse(
        Long id,
        String title,
        String description,
        String resourceType,
        String originalFilename,
        String contentType,
        String subjectName,
        String subjectScope,
        String tags,
        List<String> tagList,
        String sourceUrl,
        Long fileSize,
        OffsetDateTime createdAt
) {
    public static LearningResourceResponse from(LearningResource resource) {
        return new LearningResourceResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getResourceType(),
                resource.getOriginalFilename(),
                resource.getContentType(),
                resource.getSubjectName(),
                resource.getSubjectScope(),
                resource.getTags(),
                Arrays.stream((resource.getTags() == null ? "" : resource.getTags()).split("[,，]"))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList(),
                "EXTERNAL_COURSE".equals(resource.getResourceType()) ? resource.getStorageKey() : null,
                resource.getFileSize(),
                resource.getCreatedAt()
        );
    }
}
