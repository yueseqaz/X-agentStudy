package com.xagentstudy.rag.chunk;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/chunks")
public class KnowledgeChunkController {
    private final KnowledgeChunkService service;

    public KnowledgeChunkController(KnowledgeChunkService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeChunkResponse>> list(@PathVariable Long planId) {
        return ApiResponse.ok(service.listByPlan(planId));
    }

    @GetMapping("/search")
    public ApiResponse<List<VectorSearchResponse>> search(
            @PathVariable Long planId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.ok(service.vectorSearch(planId, query, limit));
    }
}
