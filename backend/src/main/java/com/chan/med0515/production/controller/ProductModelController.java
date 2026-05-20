package com.chan.med0515.production.controller;

import com.chan.med0515.global.response.ApiResponse;
import com.chan.med0515.material.dto.MaterialResponse;
import com.chan.med0515.production.dto.ProductModelRequest;
import com.chan.med0515.production.dto.ProductModelResponse;
import com.chan.med0515.production.service.ProductModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ProductModelController {

    private final ProductModelService modelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductModelResponse>> register(
            @Valid @RequestBody ProductModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(modelService.register(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductModelResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(modelService.findAll()));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> findMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(modelService.findMaterials(id)));
    }
}