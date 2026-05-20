package com.chan.med0515.production.repository;

import com.chan.med0515.production.entity.ProductModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductModelRepository extends JpaRepository<ProductModel, Long> {
    boolean existsByName(String name);
}