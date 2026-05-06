package com.xagentstudy.practice;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/practice-project")
public class PracticeProjectController {
    private final PracticeProjectService service;

    public PracticeProjectController(PracticeProjectService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PracticeProjectResponse> getProject(@PathVariable Long planId) {
        return ApiResponse.ok(service.getProject(planId));
    }

    @PostMapping("/submit")
    public ApiResponse<PracticeProjectResultResponse> submit(
            @PathVariable Long planId,
            @RequestBody SubmitPracticeProjectRequest request
    ) {
        return ApiResponse.ok(service.submit(planId, request));
    }
}
