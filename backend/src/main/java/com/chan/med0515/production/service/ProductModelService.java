package com.chan.med0515.production.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.material.dto.MaterialResponse;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.dto.ProductModelRequest;
import com.chan.med0515.production.dto.ProductModelResponse;
import com.chan.med0515.production.entity.ProductModel;
import com.chan.med0515.production.error.ProductionErrorCode;
import com.chan.med0515.production.repository.ProductModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductModelService {

    private final ProductModelRepository modelRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public ProductModelResponse register(ProductModelRequest request) {
        ProductModel model = ProductModel.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return ProductModelResponse.from(modelRepository.save(model));
    }

    public List<ProductModelResponse> findAll() {
        return modelRepository.findAll().stream()
                .map(ProductModelResponse::from)
                .toList();
    }

    public ProductModel getEntityById(Long id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.MODEL_NOT_FOUND));
    }

    public List<MaterialResponse> findMaterials(Long modelId) {
        getEntityById(modelId);
        return materialRepository.findByProductModelIdAndDeletedAtIsNull(modelId).stream()
                .map(MaterialResponse::from)
                .toList();
    }
}