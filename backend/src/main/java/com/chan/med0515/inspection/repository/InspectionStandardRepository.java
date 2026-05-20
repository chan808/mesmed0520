package com.chan.med0515.inspection.repository;

import com.chan.med0515.inspection.entity.InspectionStandard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionStandardRepository extends JpaRepository<InspectionStandard, Long> {

    List<InspectionStandard> findAllByMaterialId(Long materialId);
}
