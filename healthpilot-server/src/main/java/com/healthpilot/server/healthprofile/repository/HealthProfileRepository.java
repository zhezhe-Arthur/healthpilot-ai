
package com.healthpilot.server.healthprofile.repository;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthProfileRepository
        extends JpaRepository<HealthProfileEntity, Long> {

    // 查询指定用户的基础健康画像
    Optional<HealthProfileEntity> findByUserId(Long userId);

    // 创建画像前判断是否已存在
    boolean existsByUserId(Long userId);

    // 按主键和用户ID共同查询，防止跨用户访问
    Optional<HealthProfileEntity> findByIdAndUserId(
            Long id,
            Long userId
    );
}
