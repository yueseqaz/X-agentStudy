package com.xagentstudy.resource;

public record LearningResourcePreviewResponse(
        LearningResourceResponse resource,
        String previewType,
        String content
) {
}
