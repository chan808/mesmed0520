package com.chan.med0515.production.dto;

import com.chan.med0515.production.enums.InspectionResultCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InspectionResultRequest(
        @NotNull Long inspectionItemId,
        InspectionResultCode result,   // VISUAL 타입만 필수 — NUMERIC은 서버 자동 판정
        BigDecimal measuredValue,      // NUMERIC 타입만 필수
        String memo
) {
}
