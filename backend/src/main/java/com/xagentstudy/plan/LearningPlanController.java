package com.xagentstudy.plan;

import com.xagentstudy.common.response.ApiResponse;
import com.xagentstudy.plan.task.PlanTaskResponse;
import com.xagentstudy.plan.task.UpdatePlanTaskRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LearningPlanController {
    private final LearningPlanService service;

    public LearningPlanController(LearningPlanService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanSummaryResponse>> listMine() {
        return ApiResponse.ok(service.listMine());
    }

    @PostMapping("/directions/{directionId}/plans/generate")
    public ApiResponse<PlanResponse> generate(@PathVariable Long directionId) {
        return ApiResponse.ok(service.generate(directionId));
    }

    @GetMapping("/directions/{directionId}/plans/latest")
    public ApiResponse<PlanResponse> latest(@PathVariable Long directionId) {
        return ApiResponse.ok(service.latest(directionId));
    }

    @GetMapping("/plans/{planId}")
    public ApiResponse<PlanResponse> get(@PathVariable Long planId) {
        return ApiResponse.ok(service.get(planId));
    }

    @PostMapping("/plans/{planId}/share")
    public ApiResponse<PlanShareResponse> share(@PathVariable Long planId) {
        return ApiResponse.ok(service.enableShare(planId));
    }

    @GetMapping("/public/plans/{shareCode}")
    public ApiResponse<PublicPlanResponse> publicPlan(@PathVariable String shareCode) {
        return ApiResponse.ok(service.getPublicPlan(shareCode));
    }

    @PostMapping("/public/plans/{shareCode}/apply")
    public ApiResponse<PlanResponse> applyPublicPlan(@PathVariable String shareCode) {
        return ApiResponse.ok(service.applySharedPlan(shareCode));
    }

    @DeleteMapping("/plans/{planId}")
    public ApiResponse<Void> delete(@PathVariable Long planId) {
        service.delete(planId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/plans/{planId}/structure/regenerate")
    public ApiResponse<PlanResponse> regenerateStructure(@PathVariable Long planId) {
        return ApiResponse.ok(service.regenerateStructure(planId));
    }

    @GetMapping("/plans/{planId}/tasks")
    public ApiResponse<List<PlanTaskResponse>> tasks(@PathVariable Long planId) {
        return ApiResponse.ok(service.listTasks(planId));
    }

    @PatchMapping("/plans/{planId}/tasks/{stageIndex}/{taskIndex}")
    public ApiResponse<PlanTaskResponse> updateTask(
            @PathVariable Long planId,
            @PathVariable Integer stageIndex,
            @PathVariable Integer taskIndex,
            @Valid @RequestBody UpdatePlanTaskRequest request
    ) {
        return ApiResponse.ok(service.updateTask(planId, stageIndex, taskIndex, request));
    }

    @GetMapping("/plans/{planId}/adjustments")
    public ApiResponse<PlanAdjustmentResponse> adjustments(@PathVariable Long planId) {
        return ApiResponse.ok(service.adjustments(planId));
    }

    @PostMapping("/plans/{planId}/adjustments/apply")
    public ApiResponse<PlanResponse> applyAdjustment(
            @PathVariable Long planId,
            @RequestBody ApplyPlanAdjustmentRequest request
    ) {
        return ApiResponse.ok(service.applyAdjustment(planId, request));
    }
}
