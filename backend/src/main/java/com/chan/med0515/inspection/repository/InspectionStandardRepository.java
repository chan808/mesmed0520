package com.chan.med0515.inspection.repository;

import com.chan.med0515.inspection.entity.InspectionStandard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionStandardRepository extends JpaRepository<InspectionStandard, Long> {

    List<InspectionStandard> findAllByMaterialId(Long materialId);

    // 해당 품목의 최신 검사 기준서 (가장 높은 rev)
    Optional<InspectionStandard> findTopByMaterialIdOrderByRevDesc(Long materialId);
}
