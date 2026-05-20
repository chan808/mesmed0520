package com.chan.med0515.production.service;

import com.chan.med0515.production.dto.DailyDashboardResponse;
import com.chan.med0515.production.dto.DailyDashboardResponse.ModelSummary;
import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.enums.LotStatus;
import com.chan.med0515.production.repository.LotInspectionResultRepository;
import com.chan.med0515.production.repository.ProductionLotRepository;
import com.chan.med0515.production.repository.ProductionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductionPlanRepository planRepository;
    private final ProductionLotRepository lotRepository;
    private final LotInspectionResultRepository resultRepository;

    public DailyDashboardResponse getDailySummary(LocalDate date) {
        List<ProductionPlan> plans = planRepository.findByPlanDate(date);

        List<ModelSummary> summaries = plans.stream()
                .map(plan -> buildModelSummary(plan, date))
                .toList();

        int totalTarget = summaries.stream().mapToInt(ModelSummary::targetQty).sum();
        int totalPass = summaries.stream().mapToInt(ModelSummary::passCount).sum();
        double overallPassRate = totalTarget == 0 ? 0.0
                : Math.round((double) totalPass / totalTarget * 1000) / 10.0;

        return new DailyDashboardResponse(date, totalTarget, totalPass, overallPassRate, summaries);
    }

    private ModelSummary buildModelSummary(ProductionPlan plan, LocalDate date) {
        Long planId = plan.getId();
        Long modelId = plan.getModel().getId();

        int lotCount = lotRepository.countByPlanId(planId);
        int inProgressCount = lotRepository.countByPlanIdAndStatus(planId, LotStatus.IN_PROGRESS);
        int failCount = lotRepository.countByPlanIdAndStatus(planId, LotStatus.FAIL);

        long ngResultCount = resultRepository.countByPlanDateAndModelIdAndResult(
                date, modelId, InspectionResultCode.NG);
        long recheckCount = resultRepository.countRechecksByPlanDateAndModelId(date, modelId);

        double passRate = plan.getTargetQty() == 0 ? 0.0
                : Math.round((double) plan.getPassCount() / plan.getTargetQty() * 1000) / 10.0;

        return new ModelSummary(
                planId,
                modelId,
                plan.getModel().getName(),
                plan.getTargetQty(),
                plan.getPassCount(),
                lotCount,
                inProgressCount,
                failCount,
                ngResultCount,
                recheckCount,
                passRate,
                plan.getStatus()
        );
    }
}