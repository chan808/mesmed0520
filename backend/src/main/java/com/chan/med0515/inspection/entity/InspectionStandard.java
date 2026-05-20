package com.chan.med0515.inspection.entity;

import com.chan.med0515.global.entity.BaseEntity;
import com.chan.med0515.material.entity.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inspection_standard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionStandard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    // 개정 이력
    @Column(nullable = false)
    private int rev;

    @Column(nullable = false)
    private LocalDate establishedAt;

    @Column
    private String inspectionType;

    @Column
    private String inspectionLevel;

    @Column
    private String strictness;

    @Column
    private BigDecimal aql;

    @Column
    private Integer aqlAc;

    @Column
    private Integer aqlRe;

    @Builder
    public InspectionStandard(Material material, int rev, LocalDate establishedAt,
                              String inspectionType, String inspectionLevel, String strictness,
                              BigDecimal aql, Integer aqlAc, Integer aqlRe) {
        Assert.notNull(material, "자재는 필수입니다");
        Assert.notNull(establishedAt, "제정일은 필수입니다");
        this.material = material;
        this.rev = rev;
        this.establishedAt = establishedAt;
        this.inspectionType = inspectionType;
        this.inspectionLevel = inspectionLevel;
        this.strictness = strictness;
        this.aql = aql;
        this.aqlAc = aqlAc;
        this.aqlRe = aqlRe;
    }

    public void incrementRev() {
        this.rev++;
    }

    public void updateFields(LocalDate establishedAt, String inspectionType, String inspectionLevel,
                             String strictness, BigDecimal aql, Integer aqlAc, Integer aqlRe) {
        this.establishedAt = establishedAt;
        this.inspectionType = inspectionType;
        this.inspectionLevel = inspectionLevel;
        this.strictness = strictness;
        this.aql = aql;
        this.aqlAc = aqlAc;
        this.aqlRe = aqlRe;
    }
}
