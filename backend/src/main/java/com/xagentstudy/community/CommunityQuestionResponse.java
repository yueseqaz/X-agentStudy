package com.xagentstudy.community;

import java.time.OffsetDateTime;
import java.util.List;

public record CommunityQuestionResponse(
        Long id,
        Long userId,
        Long planId,
        String planTitle,
        String authorName,
        String title,
        String content,
        String tags,
        int answerCount,
        List<CommunityAnswerResponse> answers,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommunityQuestionResponse summary(CommunityQuestion question, String planTitle, String authorName, int answerCount) {
        return new CommunityQuestionResponse(
                question.getId(),
                question.getUserId(),
                question.getPlanId(),
                planTitle,
                authorName,
                question.getTitle(),
                question.getContent(),
                question.getTags(),
                answerCount,
                List.of(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    public static CommunityQuestionResponse detail(
            CommunityQuestion question,
            String planTitle,
            String authorName,
            List<CommunityAnswerResponse> answers
    ) {
        return new CommunityQuestionResponse(
                question.getId(),
                question.getUserId(),
                question.getPlanId(),
                planTitle,
                authorName,
                question.getTitle(),
                question.getContent(),
                question.getTags(),
                answers.size(),
                answers,
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
