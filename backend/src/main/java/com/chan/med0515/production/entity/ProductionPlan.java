package com.chan.med0515.production.entity;

import com.chan.med0515.global.entity.BaseEntity;
import com.chan.med0515.production.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Entity
@Table(name = "production_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ProductModel model;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    private int targetQty;

    @Column(nullable = false)
    private int passCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @Builder
    public ProductionPlan(ProductModel model, LocalDate planDate, int targetQty) {
        Assert.notNull(model, "모델은 필수입니다");
        Assert.notNull(planDate, "계획일은 필수입니다");
        Assert.isTrue(targetQty > 0, "목표수량은 1 이상이어야 합니다");
        this.model = model;
        this.planDate = planDate;
        this.targetQty = targetQty;
        this.passCount = 0;
        this.status = PlanStatus.PLANNED;
    }

    public void startIfPlanned() {
        if (this.status == PlanStatus.PLANNED) {
            this.status = PlanStatus.IN_PROGRESS;
        }
    }

    public void incrementPassCount() {
        this.passCount++;
        if (this.passCount >= this.targetQty) {
            this.status = PlanStatus.COMPLETED;
        }
    }
}