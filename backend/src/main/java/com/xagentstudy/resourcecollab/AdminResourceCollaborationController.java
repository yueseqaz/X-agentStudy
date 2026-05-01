package com.xagentstudy.resourcecollab;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.response.ApiResponse;
import com.xagentstudy.resourcecollab.response.CandidateResourceResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorApplicationResponse;
import com.xagentstudy.resourcecollab.response.IngestionSourceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/resource-collaboration")
public class AdminResourceCollaborationController {
    private final ResourceCollaborationService service;

    public AdminResourceCollaborationController(ResourceCollaborationService service) {
        this.service = service;
    }

    @GetMapping("/applications")
    public ApiResponse<List<CollaboratorApplicationResponse>> applications() {
        return ApiResponse.ok(service.listApplications());
    }

    @PostMapping("/applications/{applicationId}/review")
    public ApiResponse<CollaboratorApplicationResponse> reviewApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewCollaboratorApplicationRequest request
    ) {
        return ApiResponse.ok(service.reviewApplication(applicationId, AuthContext.currentUserId(), request));
    }

    @GetMapping("/sources")
    public ApiResponse<List<IngestionSourceResponse>> sources() {
        return ApiResponse.ok(service.adminSources());
    }

    @GetMapping("/candidates")
    public ApiResponse<List<CandidateResourceResponse>> candidates() {
        return ApiResponse.ok(service.adminCandidates());
    }

    @PostMapping("/candidates/{candidateId}/review")
    public ApiResponse<CandidateResourceResponse> reviewCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody ReviewCandidateResourceRequest request
    ) {
        return ApiResponse.ok(service.reviewCandidate(candidateId, request));
    }
}
