package com.chan.med0515.production.controller;

import com.chan.med0515.global.response.ApiResponse;
import com.chan.med0515.production.dto.*;
import com.chan.med0515.production.service.ProductionPlanService;
import com.chan.med0515.production.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionPlanService planService;
    private final ProductionService productionService;

    // ── 계획 ──────────────────────────────────────────

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<ProductionPlanResponse>> registerPlan(
            @Valid @RequestBody ProductionPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planService.register(request)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<ProductionPlanResponse>>> findPlans(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(planService.findByDate(date)));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<ProductionPlanResponse>> findPlan(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.findById(id)));
    }

    // ── Lot ───────────────────────────────────────────

    @PostMapping("/plans/{planId}/lots")
    public ResponseEntity<ApiResponse<LotDetailResponse>> startLot(@PathVariable Long planId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productionService.startLot(planId)));
    }

    @GetMapping("/lots/{lotId}")
    public ResponseEntity<ApiResponse<LotDetailResponse>> getLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.getLotDetail(lotId)));
    }

    @PostMapping("/lots/{lotId}/results")
    public ResponseEntity<ApiResponse<InspectionResultResponse>> submitResult(
            @PathVariable Long lotId,
            @Valid @RequestBody InspectionResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productionService.submitResult(lotId, request)));
    }

    @PatchMapping("/lots/{lotId}/fail")
    public ResponseEntity<ApiResponse<LotDetailResponse>> failLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.failLot(lotId)));
    }
}