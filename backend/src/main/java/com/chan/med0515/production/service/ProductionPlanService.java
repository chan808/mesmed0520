package com.chan.med0515.production.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.production.dto.ProductionPlanRequest;
import com.chan.med0515.production.dto.ProductionPlanResponse;
import com.chan.med0515.production.entity.ProductModel;
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
    private final ProductModelService modelService;

    @Transactional
    public ProductionPlanResponse register(ProductionPlanRequest request) {
        if (planRepository.existsByModelIdAndPlanDate(request.modelId(), request.planDate())) {
            throw new BusinessException(ProductionErrorCode.DUPLICATE_PLAN);
        }
        ProductModel model = modelService.getEntityById(request.modelId());
        ProductionPlan plan = ProductionPlan.builder()
                .model(model)
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

    public ProductionPlan getEntityById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.PLAN_NOT_FOUND));
    }
}
