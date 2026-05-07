package com.xagentstudy.agent.task;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class AgentTaskController {
    private final AgentTaskService service;

    public AgentTaskController(AgentTaskService service) {
        this.service = service;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AgentTaskResponse> get(@PathVariable Long taskId) {
        return ApiResponse.ok(service.get(taskId));
    }

    @GetMapping("/plans/{planId}")
    public ApiResponse<List<AgentTaskResponse>> listByPlan(@PathVariable Long planId) {
        return ApiResponse.ok(service.listByPlan(planId));
    }
}
