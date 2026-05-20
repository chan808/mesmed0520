package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.entity.InspectionStandard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InspectionStandardResponse(
        Long id,
        Long materialId,
        String modelName,
        String partName,
        int rev,
        LocalDate establishedAt,
        String inspectionType,
        String inspectionLevel,
        String strictness,
        BigDecimal aql,
        Integer aqlAc,
        Integer aqlRe
) {
    public static InspectionStandardResponse from(InspectionStandard s) {
        return new InspectionStandardResponse(
                s.getId(),
                s.getMaterial().getId(),
                s.getMaterial().getModelName(),
                s.getMaterial().getPartName(),
                s.getRev(),
                s.getEstablishedAt(),
                s.getInspectionType(),
                s.getInspectionLevel(),
                s.getStrictness(),
                s.getAql(),
                s.getAqlAc(),
                s.getAqlRe()
        );
    }
}