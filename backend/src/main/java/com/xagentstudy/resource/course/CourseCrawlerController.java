package com.xagentstudy.resource.course;

import com.xagentstudy.common.response.ApiResponse;
import com.xagentstudy.resource.LearningResourceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/course-crawler")
public class CourseCrawlerController {
    private final CourseCrawlerService service;

    public CourseCrawlerController(CourseCrawlerService service) {
        this.service = service;
    }

    @GetMapping("/candidates")
    public ApiResponse<List<CourseResourceCandidateResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping("/crawl")
    public ApiResponse<List<CourseResourceCandidateResponse>> crawl(@RequestBody CrawlCoursesRequest request) {
        return ApiResponse.ok(service.crawl(request));
    }

    @PostMapping("/candidates/{candidateId}/publish")
    public ApiResponse<LearningResourceResponse> publish(@PathVariable Long candidateId) {
        return ApiResponse.ok(service.publish(candidateId));
    }

    @PostMapping("/candidates/{candidateId}/reject")
    public ApiResponse<CourseResourceCandidateResponse> reject(@PathVariable Long candidateId) {
        return ApiResponse.ok(service.reject(candidateId));
    }
}
