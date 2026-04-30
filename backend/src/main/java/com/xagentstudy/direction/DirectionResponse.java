package com.xagentstudy.direction;

import java.time.OffsetDateTime;

public record DirectionResponse(
        Long id,
        String name,
        String category,
        String description,
        OffsetDateTime lastActiveAt
) {
    public static DirectionResponse from(LearningDirection direction) {
        return new DirectionResponse(
                direction.getId(),
                direction.getName(),
                direction.getCategory(),
                direction.getDescription(),
                direction.getLastActiveAt()
        );
    }
}
