package com.chan.med0515.production.dto;

import com.chan.med0515.production.enums.InspectionResultCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InspectionResultRequest(
        @NotNull Long inspectionItemId,
        InspectionResultCode result,
        BigDecimal measuredValue,
        String memo
) {
}
