package com.xagentstudy.report;

import java.time.LocalDate;
import java.util.List;

public record WeeklyReviewResponse(
        Long planId,
        LocalDate weekStart,
        LocalDate weekEnd,
        int completedTaskCount,
        int answeredQuestionCount,
        int correctQuestionCount,
        int wrongQuestionCount,
        double accuracyRate,
        int completedReviewCount,
        int checkinDays,
        int masteryScore,
        List<String> highlights,
        List<String> risks,
        List<String> nextWeekFocus,
        List<StudyTrendPoint> trend
) {
}
