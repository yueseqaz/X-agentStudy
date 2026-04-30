package com.xagentstudy.review;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/reviews")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/today")
    public ApiResponse<ReviewSummaryResponse> today(@PathVariable Long planId) {
        return ApiResponse.ok(service.today(planId));
    }

    @PostMapping("/{reviewId}/complete")
    public ApiResponse<ReviewRecordResponse> complete(
            @PathVariable Long planId,
            @PathVariable Long reviewId,
            @RequestBody(required = false) CompleteReviewRequest request
    ) {
        return ApiResponse.ok(service.complete(planId, reviewId, request));
    }
}
