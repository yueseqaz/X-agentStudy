package com.xagentstudy.report;

import java.util.List;

public record LearningReportResponse(
        Long planId,
        int questionCount,
        int answerAttemptCount,
        int answeredQuestionCount,
        int correctQuestionCount,
        int wrongQuestionCount,
        double accuracyRate,
        double taskCompletionRate,
        int masteryScore,
        List<String> weakPoints,
        List<String> weaknessReasons,
        List<WeakPointSourceResponse> weakPointSources,
        List<WrongQuestionResponse> wrongQuestions,
        List<String> reviewSuggestions,
        List<String> nextActions,
        List<StudyTrendPoint> trend
) {
}
