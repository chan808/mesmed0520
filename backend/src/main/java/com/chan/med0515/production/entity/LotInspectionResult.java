package com.chan.med0515.production.entity;

import com.chan.med0515.global.entity.BaseEntity;
import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.production.enums.InspectionResultCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Entity
@Table(name = "lot_inspection_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LotInspectionResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ProductionLot lot;

    // InspectionItem 단위로 결과 기록 — 항목 하나가 NG면 해당 품목 전체 재검사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_item_id", nullable = false)
    private InspectionItem inspectionItem;

    @Column(nullable = false)
    private int round;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InspectionResultCode result;

    @Column(nullable = false)
    private LocalDateTime inspectedAt;

    @Column(length = 200)
    private String memo;

    @Builder
    public LotInspectionResult(ProductionLot lot, InspectionItem inspectionItem,
                               int round, InspectionResultCode result, String memo) {
        Assert.notNull(lot, "lot은 필수입니다");
        Assert.notNull(inspectionItem, "검사항목은 필수입니다");
        Assert.notNull(result, "결과는 필수입니다");
        this.lot = lot;
        this.inspectionItem = inspectionItem;
        this.round = round;
        this.result = result;
        this.inspectedAt = LocalDateTime.now();
        this.memo = memo;
    }
}