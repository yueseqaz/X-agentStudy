package com.xagentstudy.plan;

public record PlanShareResponse(
        Long planId,
        String shareCode
) {
    public static PlanShareResponse from(LearningPlan plan) {
        return new PlanShareResponse(plan.getId(), plan.getShareCode());
    }
}
