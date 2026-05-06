package com.xagentstudy.video;

public record KnowledgeVideoContext(
        String planTitle,
        String chapterName,
        String unitName,
        String pointTitle,
        String outcome,
        String level,
        String documentContent
) {
}
