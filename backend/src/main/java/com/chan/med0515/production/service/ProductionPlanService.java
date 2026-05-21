package com.chan.med0515.production.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.dto.ProductionPlanRequest;
import com.chan.med0515.production.dto.ProductionPlanResponse;
import com.chan.med0515.production.dto.UpdateTargetQtyRequest;
import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.error.ProductionErrorCode;
import com.chan.med0515.production.repository.ProductionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionPlanService {

    private final ProductionPlanRepository planRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public ProductionPlanResponse register(ProductionPlanRequest request) {
        // material에 등록된 modelName인지 검증
        if (materialRepository.findByModelNameAndDeletedAtIsNull(request.modelName()).isEmpty()) {
            throw new BusinessException(ProductionErrorCode.MODEL_NOT_FOUND);
        }
        if (planRepository.existsByModelNameAndPlanDate(request.modelName(), request.planDate())) {
            throw new BusinessException(ProductionErrorCode.DUPLICATE_PLAN);
        }
        ProductionPlan plan = ProductionPlan.builder()
                .modelName(request.modelName())
                .planDate(request.planDate())
                .targetQty(request.targetQty())
                .build();
        return ProductionPlanResponse.from(planRepository.save(plan));
    }

    public List<ProductionPlanResponse> findByDate(LocalDate date) {
        return planRepository.findByPlanDate(date).stream()
                .map(ProductionPlanResponse::from)
                .toList();
    }

    public ProductionPlanResponse findById(Long id) {
        return planRepository.findById(id)
                .map(ProductionPlanResponse::from)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.PLAN_NOT_FOUND));
    }

    @Transactional
    public ProductionPlanResponse updateTargetQty(Long id, UpdateTargetQtyRequest request) {
        ProductionPlan plan = getEntityById(id);
        plan.updateTargetQty(request.targetQty());
        return ProductionPlanResponse.from(plan);
    }

    public ProductionPlan getEntityById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.PLAN_NOT_FOUND));
    }
}