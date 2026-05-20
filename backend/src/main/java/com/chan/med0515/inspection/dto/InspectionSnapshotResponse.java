package com.chan.med0515.inspection.dto;

import com.chan.med0515.inspection.entity.RevisionHistory;

import java.time.LocalDate;
import java.util.List;

public record InspectionSnapshotResponse(
        Long standardId,
        int rev,
        LocalDate revisionDate,
        String revisionNote,
        String confirmedBy,
        List<InspectionItemResponse> items
) {
    public static InspectionSnapshotResponse from(RevisionHistory r, List<InspectionItemResponse> items) {
        return new InspectionSnapshotResponse(
                r.getStandard().getId(),
                r.getRev(),
                r.getRevisionDate(),
                r.getRevisionNote(),
                r.getConfirmedBy(),
                items
        );
    }
}
