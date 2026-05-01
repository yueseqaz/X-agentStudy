package com.xagentstudy.resourcecollab.response;

import com.xagentstudy.resource.LearningResourceResponse;

import java.util.List;

public record CollaboratorWorkspaceResponse(
        List<IngestionSourceResponse> sources,
        List<IngestionTaskResponse> tasks,
        List<CandidateResourceResponse> candidates,
        List<LearningResourceResponse> publishedResources
) {
}
