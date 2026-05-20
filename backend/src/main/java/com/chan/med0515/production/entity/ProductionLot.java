package com.chan.med0515.production.entity;

import com.chan.med0515.global.entity.BaseEntity;
import com.chan.med0515.production.enums.LotStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "production_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionLot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan plan;

    @Column(nullable = false)
    private int lotNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LotStatus status;

    @Builder
    public ProductionLot(ProductionPlan plan, int lotNo) {
        Assert.notNull(plan, "계획은 필수입니다");
        Assert.isTrue(lotNo > 0, "lot 번호는 1 이상이어야 합니다");
        this.plan = plan;
        this.lotNo = lotNo;
        this.status = LotStatus.IN_PROGRESS;
    }

    public void pass() {
        this.status = LotStatus.PASS;
    }

    public void fail() {
        this.status = LotStatus.FAIL;
    }
}