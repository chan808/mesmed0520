package com.chan.med0515.material.controller;

import com.chan.med0515.global.response.ApiResponse;
import com.chan.med0515.material.dto.MaterialRequest;
import com.chan.med0515.material.dto.MaterialResponse;
import com.chan.med0515.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class materialController {

    private final MaterialService materialService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> register(
            @Valid @RequestBody MaterialRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(materialService.register(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(materialService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.findById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}