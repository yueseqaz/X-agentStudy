package com.xagentstudy.community;

import java.time.OffsetDateTime;

public record CommunityAnswerResponse(
        Long id,
        Long questionId,
        Long userId,
        String authorName,
        String source,
        String content,
        OffsetDateTime createdAt
) {
    public static CommunityAnswerResponse from(CommunityAnswer answer, String authorName) {
        return new CommunityAnswerResponse(
                answer.getId(),
                answer.getQuestionId(),
                answer.getUserId(),
                authorName,
                answer.getSource(),
                answer.getContent(),
                answer.getCreatedAt()
        );
    }
}
