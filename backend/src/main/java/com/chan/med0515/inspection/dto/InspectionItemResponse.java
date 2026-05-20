package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.entity.InspectionItem;

public record InspectionItemResponse(
        Long id,
        Long standardId,
        String itemName,
        String specification,
        String method,
        String equipment,
        String timing
) {
    public static InspectionItemResponse from(InspectionItem item) {
        return new InspectionItemResponse(
                item.getId(),
                item.getStandard().getId(),
                item.getItemName(),
                item.getSpecification(),
                item.getMethod(),
                item.getEquipment(),
                item.getTiming()
        );
    }
}
