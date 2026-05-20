package com.chan.med0515.production.dto;

import com.chan.med0515.production.entity.LotInspectionResult;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.enums.LotStatus;

import java.time.LocalDateTime;

public record InspectionResultResponse(
        Long resultId,
        Long lotId,
        LotStatus lotStatus,
        Long inspectionItemId,
        String itemName,
        int round,
        InspectionResultCode result,
        LocalDateTime inspectedAt,
        String memo
) {
    public static InspectionResultResponse from(LotInspectionResult r, LotStatus lotStatus) {
        return new InspectionResultResponse(
                r.getId(),
                r.getLot().getId(),
                lotStatus,
                r.getInspectionItem().getId(),
                r.getInspectionItem().getItemName(),
                r.getRound(),
                r.getResult(),
                r.getInspectedAt(),
                r.getMemo()
        );
    }
}