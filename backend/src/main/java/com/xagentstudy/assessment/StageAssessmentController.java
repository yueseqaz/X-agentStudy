package com.xagentstudy.assessment;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/stage-assessment")
public class StageAssessmentController {
    private final StageAssessmentService service;

    public StageAssessmentController(StageAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<StageAssessmentResponse> getAssessment(@PathVariable Long planId) {
        return ApiResponse.ok(service.getAssessment(planId));
    }

    @PostMapping("/submit")
    public ApiResponse<StageAssessmentResultResponse> submit(
            @PathVariable Long planId,
            @RequestBody SubmitStageAssessmentRequest request
    ) {
        return ApiResponse.ok(service.submit(planId, request));
    }
}
