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
    private final WeaknessCenterService weaknessCenterService;
    private final WeeklyReviewService weeklyReviewService;

    public LearningReportController(
            LearningReportService service,
            WeaknessCenterService weaknessCenterService,
            WeeklyReviewService weeklyReviewService
    ) {
        this.service = service;
        this.weaknessCenterService = weaknessCenterService;
        this.weeklyReviewService = weeklyReviewService;
    }

    @GetMapping
    public ApiResponse<LearningReportResponse> getReport(@PathVariable Long planId) {
        return ApiResponse.ok(service.getReport(planId));
    }

    @GetMapping("/weaknesses")
    public ApiResponse<WeaknessCenterResponse> getWeaknesses(@PathVariable Long planId) {
        return ApiResponse.ok(weaknessCenterService.getCenter(planId));
    }

    @GetMapping("/weekly-review")
    public ApiResponse<WeeklyReviewResponse> getWeeklyReview(@PathVariable Long planId) {
        return ApiResponse.ok(weeklyReviewService.getWeeklyReview(planId));
    }
}
