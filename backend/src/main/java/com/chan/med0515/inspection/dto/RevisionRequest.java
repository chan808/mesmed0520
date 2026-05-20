package com.chan.med0515.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RevisionRequest(

        @NotNull
        Long standardId,

        @NotNull
        LocalDate revisionDate,

        @NotBlank
        String revisionNote,

        String confirmedBy,

        List<InspectionItemRequest> addItems,

        List<Long> deleteItemIds
) {
}
