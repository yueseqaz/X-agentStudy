package com.xagentstudy.outcome.response;

import java.time.LocalDate;
import java.util.List;

public record LearningOutcomeResponse(
        String rangeCode,
        String rangeLabel,
        LocalDate startDate,
        LocalDate endDate,
        Overview overview,
        List<DirectionSnapshot> directions,
        List<TrendPoint> trends,
        List<Highlight> highlights,
        List<String> suggestions,
        Poster poster
) {
    public record Overview(
            int learningDays,
            long completedTaskCount,
            long completedPlanCount,
            long quizAttemptCount,
            int masteredKnowledgePointCount,
            long completedReviewCount
    ) {
    }

    public record DirectionSnapshot(
            Long directionId,
            String directionName,
            int planCount,
            double taskCompletionRate,
            double accuracyRate,
            long completedReviewCount,
            List<String> strengths,
            List<String> weakPoints
    ) {
    }

    public record TrendPoint(
            LocalDate date,
            int completedTasks,
            int quizAttempts,
            int correctAnswers,
            int completedReviews
    ) {
    }

    public record Highlight(
            String type,
            Long referenceId,
            String title,
            String subtitle,
            String summary
    ) {
    }

    public record Poster(
            String title,
            String subtitle,
            List<String> keywords,
            List<String> highlights
    ) {
    }
}
