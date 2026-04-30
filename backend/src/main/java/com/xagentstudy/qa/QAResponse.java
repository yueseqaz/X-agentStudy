package com.xagentstudy.qa;

import java.time.OffsetDateTime;

public record QAResponse(
        Long id,
        Long planId,
        String question,
        String answer,
        String citations,
        String relatedPoints,
        OffsetDateTime createdAt
) {
    public static QAResponse from(QARecord record) {
        return new QAResponse(
                record.getId(),
                record.getPlanId(),
                record.getQuestion(),
                record.getAnswer(),
                record.getCitations(),
                record.getRelatedPoints(),
                record.getCreatedAt()
        );
    }
}
