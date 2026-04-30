package com.xagentstudy.quiz;

import java.time.OffsetDateTime;

public record AnswerRecordResponse(
        Long id,
        Long questionId,
        String userAnswer,
        Boolean correct,
        Integer score,
        String feedback,
        OffsetDateTime answeredAt
) {
    public static AnswerRecordResponse from(AnswerRecord record) {
        return new AnswerRecordResponse(
                record.getId(),
                record.getQuestionId(),
                record.getUserAnswer(),
                record.getCorrect(),
                record.getScore(),
                record.getFeedback(),
                record.getAnsweredAt()
        );
    }
}
