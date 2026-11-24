
package com.healthpilot.server.healthplan.repository;

import com.healthpilot.server.healthplan.entity.HealthPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthPlanRepository
        extends JpaRepository<HealthPlanEntity, Long> {

    // 查询用户指定周的健康计划
    Optional<HealthPlanEntity> findByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 查询用户所有计划，按周开始日期倒序排列
    List<HealthPlanEntity> findByUserIdOrderByWeekStartDateDesc(
            Long userId
    );

    // 根据计划ID和用户ID查询，避免跨用户访问
    Optional<HealthPlanEntity> findByIdAndUserId(
            Long id,
            Long userId
    );

    // 判断用户指定周是否已有健康计划
    boolean existsByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );
}
