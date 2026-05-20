package com.chan.med0515.production.dto;

import com.chan.med0515.production.entity.ProductModel;

import java.time.LocalDateTime;

public record ProductModelResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static ProductModelResponse from(ProductModel model) {
        return new ProductModelResponse(
                model.getId(),
                model.getName(),
                model.getDescription(),
                model.getCreatedAt()
        );
    }
}