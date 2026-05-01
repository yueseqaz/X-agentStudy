package com.xagentstudy.resourcecollab;

import com.xagentstudy.common.response.ApiResponse;
import com.xagentstudy.resourcecollab.response.CandidateResourceResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorApplicationResponse;
import com.xagentstudy.resourcecollab.response.CollaboratorWorkspaceResponse;
import com.xagentstudy.resourcecollab.response.IngestionSourceResponse;
import com.xagentstudy.resourcecollab.response.IngestionTaskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resource-collaboration")
public class ResourceCollaborationController {
    private final ResourceCollaborationService service;

    public ResourceCollaborationController(ResourceCollaborationService service) {
        this.service = service;
    }

    @GetMapping("/application")
    public ApiResponse<CollaboratorApplicationResponse> application() {
        return ApiResponse.ok(service.currentApplication());
    }

    @PostMapping("/application")
    public ApiResponse<CollaboratorApplicationResponse> submitApplication(
            @Valid @RequestBody CollaboratorApplicationRequest request
    ) {
        return ApiResponse.ok(service.submitApplication(request));
    }

    @GetMapping("/workspace")
    public ApiResponse<CollaboratorWorkspaceResponse> workspace() {
        return ApiResponse.ok(service.workspace());
    }

    @PostMapping("/sources")
    public ApiResponse<IngestionSourceResponse> createSource(@Valid @RequestBody CreateIngestionSourceRequest request) {
        return ApiResponse.ok(service.createSource(request));
    }

    @PostMapping("/tasks")
    public ApiResponse<IngestionTaskResponse> createTask(@Valid @RequestBody CreateIngestionTaskRequest request) {
        return ApiResponse.ok(service.createTask(request));
    }

    @PatchMapping("/candidates/{candidateId}")
    public ApiResponse<CandidateResourceResponse> updateCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody UpdateCandidateResourceRequest request
    ) {
        return ApiResponse.ok(service.updateCandidate(candidateId, request));
    }

    @PostMapping("/candidates/{candidateId}/submit")
    public ApiResponse<CandidateResourceResponse> submitCandidate(@PathVariable Long candidateId) {
        return ApiResponse.ok(service.submitCandidate(candidateId));
    }
}
