package com.xagentstudy.report;

import java.time.OffsetDateTime;
import java.util.List;

public record WrongQuestionResponse(
        Long questionId,
        String stem,
        String userAnswer,
        String standardAnswer,
        String explanation,
        List<String> knowledgePoints,
        String sourceScope,
        String sourceLabel,
        OffsetDateTime answeredAt
) {
}
