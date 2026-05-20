package com.chan.med0515.inspection.entity;

import com.chan.med0515.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

@Entity
@Table(name = "inspection_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at_rev is null")
public class InspectionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false)
    private InspectionStandard standard;

    @Column(nullable = false, length = 20)
    private String itemName;

    @Column(length = 100)
    private String specification;

    @Column(length = 20)
    private String method;

    @Column(length = 20)
    private String equipment;

    @Column(length = 20)
    private String timing;

    @Column(nullable = false)
    private int addedAtRev;

    @Column
    private Integer deletedAtRev;

    @Builder
    public InspectionItem(InspectionStandard standard, String itemName, String specification,
                          String method, String equipment, String timing, int addedAtRev) {
        Assert.notNull(standard, "기준서는 필수입니다");
        Assert.hasText(itemName, "검사항목은 필수입니다");
        this.standard = standard;
        this.itemName = itemName;
        this.specification = specification;
        this.method = method;
        this.equipment = equipment;
        this.timing = timing;
        this.addedAtRev = addedAtRev;
    }

    public void softDelete(int rev) {
        this.deletedAtRev = rev;
    }
}