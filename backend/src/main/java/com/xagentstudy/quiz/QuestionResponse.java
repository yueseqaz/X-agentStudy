package com.xagentstudy.quiz;

import java.time.OffsetDateTime;

public record QuestionResponse(
        Long id,
        Long planId,
        String sourceScope,
        String quizBatchId,
        String type,
        String difficulty,
        String stem,
        String options,
        String standardAnswer,
        String explanation,
        String knowledgePoints,
        String sourceLabel,
        OffsetDateTime createdAt
) {
    public static QuestionResponse from(Question question) {
        return from(question, sourceLabel(question.getSourceScope()));
    }

    public static QuestionResponse from(Question question, String sourceLabel) {
        return new QuestionResponse(
                question.getId(),
                question.getPlanId(),
                question.getSourceScope(),
                question.getQuizBatchId(),
                question.getType(),
                question.getDifficulty(),
                question.getStem(),
                question.getOptions(),
                question.getStandardAnswer(),
                question.getExplanation(),
                question.getKnowledgePoints(),
                sourceLabel,
                question.getCreatedAt()
        );
    }

    private static String sourceLabel(String sourceScope) {
        if (sourceScope == null || sourceScope.isBlank()) {
            return "未标记来源";
        }
        if ("COLD_START".equals(sourceScope)) {
            return "无知识库通用题";
        }
        if (sourceScope.startsWith("CHUNK:")) {
            return "资料切片 #" + sourceScope.substring("CHUNK:".length());
        }
        if (sourceScope.startsWith("DOCUMENT:")) {
            return "学习文档 #" + sourceScope.substring("DOCUMENT:".length());
        }
        if (sourceScope.startsWith("RAG:")) {
            return "多资料切片 " + sourceScope.substring("RAG:".length());
        }
        return sourceScope;
    }
}
