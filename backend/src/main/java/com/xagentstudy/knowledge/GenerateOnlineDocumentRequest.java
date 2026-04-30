package com.xagentstudy.knowledge;

public record GenerateOnlineDocumentRequest(
        String knowledgePointId,
        String title,
        String chapterName,
        String unitName,
        String level,
        String outcome,
        String documentStyle
) {
}
