package com.chan.med0515.production.dto;

import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.enums.PlanStatus;

import java.time.LocalDate;

public record ProductionPlanResponse(
        Long id,
        Long modelId,
        String modelName,
        LocalDate planDate,
        int targetQty,
        int passCount,
        PlanStatus status
) {
    public static ProductionPlanResponse from(ProductionPlan plan) {
        return new ProductionPlanResponse(
                plan.getId(),
                plan.getModel().getId(),
                plan.getModel().getName(),
                plan.getPlanDate(),
                plan.getTargetQty(),
                plan.getPassCount(),
                plan.getStatus()
        );
    }
}