package com.chan.med0515.material.entity;

import com.chan.med0515.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String modelName;

    @Column(nullable = false, length = 50)
    private String partName;

    @Column(length = 50)
    private String partCode;

    @Column(length = 50)
    private String supplier;

    @Column(length = 20)
    private String materialSpec;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Material(String modelName, String partName, String partCode, String supplier, String materialSpec) {
        Assert.hasText(modelName, "모델명은 필수입니다");
        Assert.hasText(partName, "품명은 필수입니다");

        this.modelName = modelName;
        this.partName = partName;
        this.partCode = partCode;
        this.supplier = supplier;
        this.materialSpec = materialSpec;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}
