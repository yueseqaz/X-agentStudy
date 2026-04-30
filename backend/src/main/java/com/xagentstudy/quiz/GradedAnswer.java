package com.xagentstudy.quiz;

public record GradedAnswer(
        boolean correct,
        int score,
        String feedback
) {
}
