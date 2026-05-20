package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.entity.RevisionHistory;

import java.time.LocalDate;
import java.util.List;

public record RevisionHistoryResponse(
        Long id,
        Long standardId,
        int rev,
        LocalDate revisionDate,
        String revisionNote,
        String confirmedBy,
        List<InspectionItemResponse> addedItems
) {
    public static RevisionHistoryResponse from(RevisionHistory r, List<InspectionItemResponse> addedItems) {
        return new RevisionHistoryResponse(
                r.getId(),
                r.getStandard().getId(),
                r.getRev(),
                r.getRevisionDate(),
                r.getRevisionNote(),
                r.getConfirmedBy(),
                addedItems
        );
    }
}