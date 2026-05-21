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

    @Column(nullable = false, length = 50)
    private String modelName;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    private int targetQty;

    @Column(nullable = false)
    private int passCount;

    @Column(nullable = false)
    private int failCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @Builder
    public ProductionPlan(String modelName, LocalDate planDate, int targetQty) {
        Assert.hasText(modelName, "모델명은 필수입니다");
        Assert.notNull(planDate, "계획일은 필수입니다");
        Assert.isTrue(targetQty > 0, "목표수량은 1 이상이어야 합니다");
        this.modelName = modelName;
        this.planDate = planDate;
        this.targetQty = targetQty;
        this.passCount = 0;
        this.failCount = 0;
        this.status = PlanStatus.PLANNED;
    }

    public void startIfPlanned() {
        if (this.status == PlanStatus.PLANNED) {
            this.status = PlanStatus.IN_PROGRESS;
        }
    }

    public void updateTargetQty(int qty) {
        Assert.isTrue(qty > 0, "목표수량은 1 이상이어야 합니다");
        this.targetQty = qty;
        if (this.passCount >= this.targetQty) {
            this.status = PlanStatus.COMPLETED;
        }
    }

    public void incrementFailCount() {
        this.failCount++;
    }

    public void incrementPassCount() {
        this.passCount++;
        if (this.passCount >= this.targetQty) {
            this.status = PlanStatus.COMPLETED;
        }
    }
}