package com.xagentstudy.plan.task;

import jakarta.validation.constraints.NotNull;

public record UpdatePlanTaskRequest(
        @NotNull Boolean completed
) {
}
