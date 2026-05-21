package com.chan.med0515.production.repository;

import com.chan.med0515.production.entity.LotInspectionResult;
import com.chan.med0515.production.enums.InspectionResultCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LotInspectionResultRepository extends JpaRepository<LotInspectionResult, Long> {

    List<LotInspectionResult> findByLotIdAndInspectionItemIdOrderByRoundAsc(Long lotId, Long itemId);

    int countByLotIdAndInspectionItemId(Long lotId, Long itemId);

    Optional<LotInspectionResult> findTopByLotIdAndInspectionItemIdOrderByRoundDesc(Long lotId, Long itemId);

    // 대시보드: 특정 날짜, 특정 모델의 총 NG 건수
    @Query("""
            SELECT COUNT(r) FROM LotInspectionResult r
            WHERE r.lot.plan.planDate = :date
              AND r.lot.plan.modelName = :modelName
              AND r.result = :result
            """)
    long countByPlanDateAndModelNameAndResult(
            @Param("date") LocalDate date,
            @Param("modelName") String modelName,
            @Param("result") InspectionResultCode result);

    // 대시보드: 재검사 횟수 (round > 1인 결과 수)
    @Query("""
            SELECT COUNT(r) FROM LotInspectionResult r
            WHERE r.lot.plan.planDate = :date
              AND r.lot.plan.modelName = :modelName
              AND r.round > 1
            """)
    long countRechecksByPlanDateAndModelName(
            @Param("date") LocalDate date,
            @Param("modelName") String modelName);
}