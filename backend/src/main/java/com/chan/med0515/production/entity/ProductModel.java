package com.chan.med0515.production.entity;

import com.chan.med0515.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "product_model")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductModel extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Builder
    public ProductModel(String name, String description) {
        Assert.hasText(name, "모델명은 필수입니다");
        this.name = name;
        this.description = description;
    }
}