package com.chan.med0515.production.dto;

import com.chan.med0515.production.enums.PlanStatus;

import java.time.LocalDate;
import java.util.List;

public record DailyDashboardResponse(
        LocalDate date,
        int totalTargetQty,
        int totalPassCount,
        double overallPassRate,
        List<ModelSummary> byModel
) {

    public record ModelSummary(
            Long planId,
            Long modelId,
            String modelName,
            int targetQty,
            int passCount,
            int lotCount,
            int inProgressCount,
            int failCount,
            long ngResultCount,   // 전체 NG 판정 건수
            long recheckCount,    // 재검사 횟수 (round > 1)
            double passRate,
            PlanStatus planStatus
    ) {
    }
}