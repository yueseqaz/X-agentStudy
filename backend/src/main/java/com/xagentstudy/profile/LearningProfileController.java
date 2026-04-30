package com.xagentstudy.profile;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/directions/{directionId}/profile")
public class LearningProfileController {
    private final LearningProfileService service;

    public LearningProfileController(LearningProfileService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    public ApiResponse<ProfileResponse> latest(@PathVariable Long directionId) {
        return ApiResponse.ok(service.latest(directionId));
    }

    @PostMapping("/generate")
    public ApiResponse<ProfileResponse> generate(
            @PathVariable Long directionId,
            @Valid @RequestBody GenerateProfileRequest request
    ) {
        return ApiResponse.ok(service.generate(directionId, request));
    }
}
