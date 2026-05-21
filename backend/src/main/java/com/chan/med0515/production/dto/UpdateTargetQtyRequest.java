package com.chan.med0515.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTargetQtyRequest(
        @NotNull @Min(1) Integer targetQty
) {}
