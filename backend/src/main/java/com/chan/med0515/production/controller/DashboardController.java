package com.chan.med0515.production.controller;

import com.chan.med0515.global.response.ApiResponse;
import com.chan.med0515.production.dto.DailyDashboardResponse;
import com.chan.med0515.production.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyDashboardResponse>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDailySummary(target)));
    }
}