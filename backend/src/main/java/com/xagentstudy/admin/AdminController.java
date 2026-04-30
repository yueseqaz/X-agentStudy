package com.xagentstudy.admin;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> summary() {
        return ApiResponse.ok(service.summary());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.ok(service.users());
    }

    @PatchMapping("/users/{userId}/disabled")
    public ApiResponse<AdminUserResponse> setUserDisabled(
            @PathVariable Long userId,
            @RequestBody AdminSetUserDisabledRequest request
    ) {
        return ApiResponse.ok(service.setUserDisabled(userId, Boolean.TRUE.equals(request.disabled()), request.disabledUntil()));
    }

    @GetMapping("/tasks/recent")
    public ApiResponse<List<AdminTaskResponse>> recentTasks() {
        return ApiResponse.ok(service.recentTasks());
    }

    @GetMapping("/tasks/failed")
    public ApiResponse<List<AdminTaskResponse>> failedTasks() {
        return ApiResponse.ok(service.failedTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<AdminTaskResponse> taskDetail(@PathVariable Long taskId) {
        return ApiResponse.ok(service.taskDetail(taskId));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<AdminTaskResponse> retryTask(@PathVariable Long taskId) {
        return ApiResponse.ok(service.retryTask(taskId));
    }

    @GetMapping("/quotas")
    public ApiResponse<List<AdminQuotaResponse>> quotas() {
        return ApiResponse.ok(service.quotas());
    }

    @PostMapping("/quotas/{userId}/reset")
    public ApiResponse<AdminQuotaResponse> resetQuota(@PathVariable Long userId) {
        return ApiResponse.ok(service.resetQuota(userId));
    }

    @PostMapping("/quotas/{userId}/wallet/recharge")
    public ApiResponse<AdminQuotaResponse> rechargeWallet(
            @PathVariable Long userId,
            @Valid @RequestBody AdminRechargeWalletRequest request
    ) {
        return ApiResponse.ok(service.rechargeWallet(userId, request.amountCents(), request.remark()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AdminAuditLogResponse>> auditLogs() {
        return ApiResponse.ok(service.auditLogs());
    }
}
