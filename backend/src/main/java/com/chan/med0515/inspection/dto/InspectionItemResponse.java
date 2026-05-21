package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.enums.MeasurementType;

import java.math.BigDecimal;

public record InspectionItemResponse(
        Long id,
        Long standardId,
        String itemName,
        String specification,
        String method,
        String equipment,
        String timing,
        MeasurementType measurementType,
        BigDecimal minValue,
        BigDecimal maxValue,
        String unit
) {
    public static InspectionItemResponse from(InspectionItem item) {
        return new InspectionItemResponse(
                item.getId(),
                item.getStandard().getId(),
                item.getItemName(),
                item.getSpecification(),
                item.getMethod(),
                item.getEquipment(),
                item.getTiming(),
                item.getMeasurementType(),
                item.getMinValue(),
                item.getMaxValue(),
                item.getUnit()
        );
    }
}
