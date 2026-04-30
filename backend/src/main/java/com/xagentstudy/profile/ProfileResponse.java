package com.xagentstudy.profile;

import java.time.OffsetDateTime;

public record ProfileResponse(
        Long id,
        Long directionId,
        String goal,
        String currentLevel,
        String timeBudget,
        String preference,
        String risks,
        String strategy,
        String rawConversation,
        OffsetDateTime createdAt
) {
    public static ProfileResponse from(LearningProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getDirectionId(),
                profile.getGoal(),
                profile.getCurrentLevel(),
                profile.getTimeBudget(),
                profile.getPreference(),
                profile.getRisks(),
                profile.getStrategy(),
                profile.getRawConversation(),
                profile.getCreatedAt()
        );
    }
}
