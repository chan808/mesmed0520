package com.chan.med0515.production.repository;

import com.chan.med0515.production.entity.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {
    List<ProductionPlan> findByPlanDate(LocalDate date);
    boolean existsByModelNameAndPlanDate(String modelName, LocalDate planDate);
}