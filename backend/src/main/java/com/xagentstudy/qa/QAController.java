package com.xagentstudy.qa;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/qa")
public class QAController {
    private final QAService service;

    public QAController(QAService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<QAResponse> ask(@PathVariable Long planId, @Valid @RequestBody AskQuestionRequest request) {
        return ApiResponse.ok(service.ask(planId, request));
    }

    @GetMapping
    public ApiResponse<List<QAResponse>> history(@PathVariable Long planId) {
        return ApiResponse.ok(service.history(planId));
    }
}
