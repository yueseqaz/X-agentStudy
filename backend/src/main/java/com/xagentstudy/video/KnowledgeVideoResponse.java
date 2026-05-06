package com.xagentstudy.video;

import java.util.List;

public record KnowledgeVideoResponse(
        String videoId,
        String title,
        String status,
        String message,
        String sourceCode,
        String videoUrl,
        List<RemotionScene> scenes
) {
}
