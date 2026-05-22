package com.chan.med0515.production.repository;

import com.chan.med0515.production.entity.ProductionLot;
import com.chan.med0515.production.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionLotRepository extends JpaRepository<ProductionLot, Long> {
    List<ProductionLot> findByPlanId(Long planId);
    int countByPlanId(Long planId);
    int countByPlanIdAndStatus(Long planId, LotStatus status);
    Optional<ProductionLot> findFirstByPlanIdAndStatusOrderByCreatedAtDesc(Long planId, LotStatus status);
}