package com.xagentstudy.outcome;

import com.xagentstudy.common.response.ApiResponse;
import com.xagentstudy.outcome.response.LearningOutcomeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/outcomes")
public class LearningOutcomeController {
    private final LearningOutcomeService service;

    public LearningOutcomeController(LearningOutcomeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<LearningOutcomeResponse> getOutcomes(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ApiResponse.ok(service.getOutcomes(range, startDate, endDate));
    }
}
