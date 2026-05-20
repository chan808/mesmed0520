package com.chan.med0515.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProductionPlanRequest(
        @NotNull Long modelId,
        @NotNull LocalDate planDate,
        @Min(1) int targetQty
) {
}