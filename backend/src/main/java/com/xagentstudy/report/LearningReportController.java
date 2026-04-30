package com.xagentstudy.report;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/report")
public class LearningReportController {
    private final LearningReportService service;

    public LearningReportController(LearningReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<LearningReportResponse> getReport(@PathVariable Long planId) {
        return ApiResponse.ok(service.getReport(planId));
    }
}
