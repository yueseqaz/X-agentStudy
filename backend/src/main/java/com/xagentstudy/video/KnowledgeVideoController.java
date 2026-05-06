package com.xagentstudy.video;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans/{planId}/knowledge-videos")
public class KnowledgeVideoController {
    private final KnowledgeVideoService service;

    public KnowledgeVideoController(KnowledgeVideoService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public ApiResponse<KnowledgeVideoResponse> generate(
            @PathVariable Long planId,
            @RequestBody GenerateKnowledgeVideoRequest request
    ) {
        return ApiResponse.ok(service.generate(planId, request));
    }

    @GetMapping("/{videoId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long planId, @PathVariable String videoId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"knowledge-video.mp4\"")
                .body(service.videoFile(planId, videoId));
    }
}
