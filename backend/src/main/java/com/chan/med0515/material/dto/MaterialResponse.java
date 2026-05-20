package com.chan.med0515.material.dto;

import com.chan.med0515.material.entity.Material;

public record MaterialResponse(
        Long id,
        String modelName,
        String partName,
        String partCode,
        String suppier,
        String materialSpec
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getModelName(),
                material.getPartName(),
                material.getPartCode(),
                material.getSupplier(),
                material.getMaterialSpec()
        );
    }
}
