package com.xagentstudy.report;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}")
public class PlanInsightController {
    private final WeaknessCenterService weaknessCenterService;
    private final WeeklyReviewService weeklyReviewService;
    private final DynamicProfileService dynamicProfileService;
    private final ResourceQualityService resourceQualityService;

    public PlanInsightController(
            WeaknessCenterService weaknessCenterService,
            WeeklyReviewService weeklyReviewService,
            DynamicProfileService dynamicProfileService,
            ResourceQualityService resourceQualityService
    ) {
        this.weaknessCenterService = weaknessCenterService;
        this.weeklyReviewService = weeklyReviewService;
        this.dynamicProfileService = dynamicProfileService;
        this.resourceQualityService = resourceQualityService;
    }

    @GetMapping("/weaknesses")
    public ApiResponse<WeaknessCenterResponse> getWeaknesses(@PathVariable Long planId) {
        return ApiResponse.ok(weaknessCenterService.getCenter(planId));
    }

    @GetMapping("/weekly-review")
    public ApiResponse<WeeklyReviewResponse> getWeeklyReview(@PathVariable Long planId) {
        return ApiResponse.ok(weeklyReviewService.getWeeklyReview(planId));
    }

    @GetMapping("/dynamic-profile")
    public ApiResponse<DynamicProfileResponse> getDynamicProfile(@PathVariable Long planId) {
        return ApiResponse.ok(dynamicProfileService.getDynamicProfile(planId));
    }

    @GetMapping("/resource-quality")
    public ApiResponse<ResourceQualityResponse> getResourceQuality(@PathVariable Long planId) {
        return ApiResponse.ok(resourceQualityService.getQuality(planId));
    }
}
