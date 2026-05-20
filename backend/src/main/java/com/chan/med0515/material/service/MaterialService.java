package com.chan.med0515.material.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.material.dto.MaterialRequest;
import com.chan.med0515.material.dto.MaterialResponse;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.error.MaterialErrorCode;
import com.chan.med0515.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional
    public MaterialResponse register(MaterialRequest request) {
        Material material = Material.builder()
                .modelName(request.modelName())
                .partName(request.partName())
                .partCode(request.partCode())
                .supplier(request.supplier())
                .materialSpec(request.materialSpec())
                .build();
        return MaterialResponse.from(materialRepository.save(material));
    }

    public List<MaterialResponse> findAll() {
        return materialRepository.findAll().stream()
                .map(MaterialResponse::from)
                .toList();
    }

    public MaterialResponse findById(Long id) {
        return materialRepository.findById(id)
                .map(MaterialResponse::from)
                .orElseThrow(() -> new BusinessException(MaterialErrorCode.MATERIAL_NOT_FOUND));
    }

    @Transactional
    public void delete(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        material.softDelete();
    }

    // 다른 도메인 Service에서만 호출 — Controller에 노출하지 않음
    public Material getEntityById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MaterialErrorCode.MATERIAL_NOT_FOUND));
    }
}
