package com.chan.med0515.inspection.repository;

import com.chan.med0515.inspection.entity.InspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {

    List<InspectionItem> findAllByStandardId(Long standardId);

    // @SQLRestriction 우회 — 특정 rev 시점의 활성 항목 조회
    @Query(value = """
            SELECT * FROM inspection_item
            WHERE standard_id = :standardId
              AND added_at_rev <= :rev
              AND (deleted_at_rev IS NULL OR deleted_at_rev > :rev)
            """, nativeQuery = true)
    List<InspectionItem> findSnapshotItems(@Param("standardId") Long standardId, @Param("rev") int rev);
}
