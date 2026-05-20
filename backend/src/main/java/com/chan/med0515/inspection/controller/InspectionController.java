package com.chan.med0515.inspection.controller;

import com.chan.med0515.global.response.ApiResponse;
import com.chan.med0515.inspection.dto.*;
import com.chan.med0515.inspection.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/standards")
    public ResponseEntity<ApiResponse<InspectionStandardResponse>> registerStandard(
            @Valid @RequestBody InspectionStandardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inspectionService.registerStandard(request)));
    }

    @PatchMapping("/standards/{id}")
    public ResponseEntity<ApiResponse<InspectionStandardResponse>> updateStandard(
            @PathVariable Long id,
            @Valid @RequestBody InspectionStandardUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.updateStandard(id, request)));
    }

    @GetMapping("/standards")
    public ResponseEntity<ApiResponse<List<InspectionStandardResponse>>> findStandardsByMaterial(
            @RequestParam Long materialId) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.findStandardsByMaterial(materialId)));
    }

    @GetMapping("/standards/{id}")
    public ResponseEntity<ApiResponse<InspectionStandardResponse>> findStandardById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.findStandardById(id)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<InspectionItemResponse>>> findItemsByStandard(
            @RequestParam Long standardId) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.findItemsByStandard(standardId)));
    }

    @PostMapping("/revisions")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> addRevision(
            @Valid @RequestBody RevisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inspectionService.addRevision(request)));
    }

    @GetMapping("/revisions")
    public ResponseEntity<ApiResponse<List<RevisionHistoryResponse>>> findRevisionsByStandard(
            @RequestParam Long standardId) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.findRevisionsByStandard(standardId)));
    }

    @GetMapping("/standards/{id}/snapshot")
    public ResponseEntity<ApiResponse<InspectionSnapshotResponse>> getSnapshotByRev(
            @PathVariable Long id,
            @RequestParam int rev) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.getSnapshotByRev(id, rev)));
    }
}