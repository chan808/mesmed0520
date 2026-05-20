package com.chan.med0515.production.dto;

import com.chan.med0515.production.enums.InspectionResultCode;
import jakarta.validation.constraints.NotNull;

public record InspectionResultRequest(
        @NotNull Long inspectionItemId,
        @NotNull InspectionResultCode result,
        String memo
) {
}