package com.xagentstudy.billing;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public ApiResponse<BillingResponse> current() {
        return ApiResponse.ok(service.current());
    }

    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanResponse>> plans() {
        return ApiResponse.ok(service.plans());
    }

    @PostMapping("/simulate-change")
    public ApiResponse<BillingResponse> simulateChange(@Valid @RequestBody ChangeSubscriptionRequest request) {
        return ApiResponse.ok(service.simulateChange(request));
    }

    @PostMapping("/purchase")
    public ApiResponse<BillingResponse> purchase(@Valid @RequestBody ChangeSubscriptionRequest request) {
        return ApiResponse.ok(service.purchase(request));
    }
}
