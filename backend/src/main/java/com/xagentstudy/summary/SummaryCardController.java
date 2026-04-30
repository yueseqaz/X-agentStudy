package com.xagentstudy.summary;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/summary-cards")
public class SummaryCardController {
    private final SummaryCardService service;

    public SummaryCardController(SummaryCardService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SummaryCardResponse>> list(@PathVariable Long planId) {
        return ApiResponse.ok(service.list(planId));
    }

    @GetMapping("/{cardId}")
    public ApiResponse<SummaryCardResponse> detail(@PathVariable Long planId, @PathVariable Long cardId) {
        return ApiResponse.ok(service.detail(planId, cardId));
    }

    @PostMapping("/documents/{documentId}/generate")
    public ApiResponse<SummaryCardResponse> generate(@PathVariable Long planId, @PathVariable Long documentId) {
        return ApiResponse.ok(service.generate(planId, documentId));
    }

    @PostMapping("/{cardId}/regenerate")
    public ApiResponse<SummaryCardResponse> regenerate(@PathVariable Long planId, @PathVariable Long cardId) {
        return ApiResponse.ok(service.regenerate(planId, cardId));
    }

    @PutMapping("/{cardId}/image")
    public ApiResponse<SummaryCardResponse> saveImage(
            @PathVariable Long planId,
            @PathVariable Long cardId,
            @RequestBody SaveSummaryCardImageRequest request
    ) {
        return ApiResponse.ok(service.saveImage(planId, cardId, request));
    }

    @DeleteMapping("/{cardId}")
    public ApiResponse<Void> delete(@PathVariable Long planId, @PathVariable Long cardId) {
        service.delete(planId, cardId);
        return ApiResponse.ok(null);
    }
}
