package com.chan.med0515.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProductionPlanRequest(
        @NotBlank String modelName,
        @NotNull LocalDate planDate,
        @Min(1) int targetQty
) {
}