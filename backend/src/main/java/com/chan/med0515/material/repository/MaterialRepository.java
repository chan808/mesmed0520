package com.chan.med0515.material.repository;

import com.chan.med0515.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByPartCode(String partCode);
}
