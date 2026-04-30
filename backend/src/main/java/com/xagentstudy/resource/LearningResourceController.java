package com.xagentstudy.resource;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LearningResourceController {
    private final LearningResourceService service;

    public LearningResourceController(LearningResourceService service) {
        this.service = service;
    }

    @GetMapping("/resources")
    public ApiResponse<List<LearningResourceResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/resources/{resourceId}")
    public ApiResponse<LearningResourceResponse> detail(@PathVariable Long resourceId) {
        return ApiResponse.ok(service.detail(resourceId));
    }

    @GetMapping("/resources/{resourceId}/preview")
    public ApiResponse<LearningResourcePreviewResponse> preview(@PathVariable Long resourceId) {
        return ApiResponse.ok(service.preview(resourceId));
    }

    @GetMapping("/resources/{resourceId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long resourceId) {
        return service.file(resourceId);
    }

    @GetMapping("/plans/{planId}/resources/recommended")
    public ApiResponse<List<LearningResourceResponse>> recommended(@PathVariable Long planId) {
        return ApiResponse.ok(service.recommended(planId));
    }

    @PostMapping("/admin/resources")
    public ApiResponse<LearningResourceResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("subjectName") String subjectName,
            @RequestParam("subjectScope") String subjectScope,
            @RequestParam("tags") String tags
    ) {
        return ApiResponse.ok(service.upload(file, title, description, subjectName, subjectScope, tags));
    }

    @DeleteMapping("/admin/resources/{resourceId}")
    public ApiResponse<Void> delete(@PathVariable Long resourceId) {
        service.delete(resourceId);
        return ApiResponse.ok(null);
    }
}
