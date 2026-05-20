package com.chan.med0515.production.dto;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.production.entity.LotInspectionResult;
import com.chan.med0515.production.entity.ProductionLot;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.enums.LotStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LotDetailResponse(
        Long lotId,
        int lotNo,
        LotStatus status,
        Long planId,
        LocalDate planDate,
        String modelName,
        List<MaterialResult> materials
) {

    public record MaterialResult(
            Long materialId,
            String partName,
            String partCode,
            // null = 미검사, PASS = 전항목 합격, NG = 하나 이상 불합격
            InspectionResultCode materialResult,
            List<ItemResult> items
    ) {
    }

    public record ItemResult(
            Long itemId,
            String itemName,
            String specification,
            InspectionResultCode currentResult, // null = 미검사
            int latestRound,
            List<RoundResult> rounds
    ) {
        public static ItemResult from(InspectionItem item, List<LotInspectionResult> results) {
            InspectionResultCode current = results.isEmpty() ? null
                    : results.get(results.size() - 1).getResult();
            int latestRound = results.isEmpty() ? 0 : results.get(results.size() - 1).getRound();
            List<RoundResult> rounds = results.stream().map(RoundResult::from).toList();
            return new ItemResult(item.getId(), item.getItemName(), item.getSpecification(),
                    current, latestRound, rounds);
        }
    }

    public record RoundResult(
            int round,
            InspectionResultCode result,
            LocalDateTime inspectedAt,
            String memo
    ) {
        public static RoundResult from(LotInspectionResult r) {
            return new RoundResult(r.getRound(), r.getResult(), r.getInspectedAt(), r.getMemo());
        }
    }

    public static LotDetailResponse from(ProductionLot lot, List<MaterialResult> materials) {
        return new LotDetailResponse(
                lot.getId(),
                lot.getLotNo(),
                lot.getStatus(),
                lot.getPlan().getId(),
                lot.getPlan().getPlanDate(),
                lot.getPlan().getModel().getName(),
                materials
        );
    }

    public static MaterialResult toMaterialResult(Material material,
                                                   InspectionResultCode materialResult,
                                                   List<ItemResult> items) {
        return new MaterialResult(
                material.getId(),
                material.getPartName(),
                material.getPartCode(),
                materialResult,
                items
        );
    }
}