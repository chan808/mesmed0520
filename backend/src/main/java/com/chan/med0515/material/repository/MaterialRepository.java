package com.chan.med0515.material.repository;

import com.chan.med0515.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByPartCode(String partCode);

    List<Material> findByProductModelIdAndDeletedAtIsNull(Long productModelId);
}
