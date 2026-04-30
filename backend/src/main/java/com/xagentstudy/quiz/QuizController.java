package com.xagentstudy.quiz;

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
@RequestMapping("/api/v1/plans/{planId}")
public class QuizController {
    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @PostMapping("/quizzes/generate")
    public ApiResponse<List<QuestionResponse>> generate(
            @PathVariable Long planId,
            @Valid @RequestBody GenerateQuizRequest request
    ) {
        return ApiResponse.ok(service.generate(planId, request));
    }

    @GetMapping("/questions")
    public ApiResponse<List<QuestionResponse>> list(@PathVariable Long planId) {
        return ApiResponse.ok(service.list(planId));
    }

    @GetMapping("/questions/wrong")
    public ApiResponse<List<QuestionResponse>> listWrong(@PathVariable Long planId) {
        return ApiResponse.ok(service.listWrong(planId));
    }

    @GetMapping("/documents/{documentId}/questions")
    public ApiResponse<List<QuestionResponse>> listDocumentQuestions(
            @PathVariable Long planId,
            @PathVariable Long documentId
    ) {
        return ApiResponse.ok(service.listDocumentQuestions(planId, documentId));
    }

    @PostMapping("/questions/{questionId}/answers")
    public ApiResponse<AnswerRecordResponse> answer(
            @PathVariable Long planId,
            @PathVariable Long questionId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return ApiResponse.ok(service.answer(planId, questionId, request));
    }
}
