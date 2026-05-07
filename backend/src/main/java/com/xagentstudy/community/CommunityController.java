package com.xagentstudy.community;

import com.xagentstudy.auth.AuthContext;
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
@RequestMapping("/api/v1/community/questions")
public class CommunityController {
    private final CommunityService service;

    public CommunityController(CommunityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CommunityQuestionResponse>> list() {
        return ApiResponse.ok(service.listQuestions());
    }

    @PostMapping
    public ApiResponse<CommunityQuestionResponse> create(@Valid @RequestBody CreateCommunityQuestionRequest request) {
        return ApiResponse.ok(service.createQuestion(AuthContext.currentUserId(), request));
    }

    @GetMapping("/{questionId}")
    public ApiResponse<CommunityQuestionResponse> get(@PathVariable Long questionId) {
        return ApiResponse.ok(service.getQuestion(questionId));
    }

    @PostMapping("/{questionId}/answers")
    public ApiResponse<CommunityAnswerResponse> answer(
            @PathVariable Long questionId,
            @Valid @RequestBody CreateCommunityAnswerRequest request
    ) {
        return ApiResponse.ok(service.createAnswer(AuthContext.currentUserId(), questionId, request));
    }

    @PostMapping("/{questionId}/ai-answer")
    public ApiResponse<CommunityAnswerResponse> aiAnswer(@PathVariable Long questionId) {
        return ApiResponse.ok(service.createAiAnswer(questionId, "用户点击邀请 AI 回答"));
    }
}
