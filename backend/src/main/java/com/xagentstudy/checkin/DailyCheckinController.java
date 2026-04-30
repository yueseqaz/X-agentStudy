package com.xagentstudy.checkin;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/checkins")
public class DailyCheckinController {
    private final DailyCheckinService service;

    public DailyCheckinController(DailyCheckinService service) {
        this.service = service;
    }

    @GetMapping("/month")
    public ApiResponse<CheckinMonthResponse> month(@RequestParam String month) {
        return ApiResponse.ok(service.month(month));
    }

    @GetMapping("/{date}")
    public ApiResponse<CheckinDayResponse> day(@PathVariable LocalDate date) {
        return ApiResponse.ok(service.day(date));
    }

    @PostMapping("/{date}")
    public ApiResponse<CheckinDayResponse> save(@PathVariable LocalDate date, @RequestBody SaveCheckinRequest request) {
        return ApiResponse.ok(service.save(date, request));
    }
}
