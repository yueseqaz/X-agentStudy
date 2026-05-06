package com.xagentstudy.dashboard;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;
    private final LearningCoachService coachService;
    private final DailyStudyWorkspaceService workspaceService;

    public DashboardController(DashboardService service, LearningCoachService coachService, DailyStudyWorkspaceService workspaceService) {
        this.service = service;
        this.coachService = coachService;
        this.workspaceService = workspaceService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.ok(service.getSummary());
    }

    @GetMapping("/coach/today")
    public ApiResponse<LearningCoachResponse> getTodayCoach() {
        return ApiResponse.ok(coachService.today());
    }

    @GetMapping("/workspace/today")
    public ApiResponse<DailyStudyWorkspaceResponse> getTodayWorkspace() {
        return ApiResponse.ok(workspaceService.today());
    }
}
