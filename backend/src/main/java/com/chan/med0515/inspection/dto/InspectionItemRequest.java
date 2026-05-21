package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.enums.MeasurementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InspectionItemRequest(

        @NotBlank
        String itemName,

        String specification,

        String method,

        String equipment,

        String timing,

        @NotNull
        MeasurementType measurementType,

        BigDecimal minValue,

        BigDecimal maxValue,

        String unit
) {
}
