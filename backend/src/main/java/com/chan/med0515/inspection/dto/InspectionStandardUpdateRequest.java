package com.chan.med0515.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InspectionStandardUpdateRequest(

        @NotNull
        LocalDate establishedAt,

        String inspectionType,
        String inspectionLevel,
        String strictness,
        BigDecimal aql,
        Integer aqlAc,
        Integer aqlRe,

        @NotNull
        LocalDate revisionDate,

        @NotBlank
        String revisionNote,

        String confirmedBy
) {
}