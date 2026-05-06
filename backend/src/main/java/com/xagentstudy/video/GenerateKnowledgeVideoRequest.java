package com.xagentstudy.video;

public record GenerateKnowledgeVideoRequest(
        String knowledgePointId,
        String title,
        String chapterName,
        String unitName,
        String level,
        String outcome,
        String documentContent
) {
}
