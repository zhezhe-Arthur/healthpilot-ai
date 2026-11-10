
package com.healthpilot.server.healthcheckin.repository;

import com.healthpilot.server.healthcheckin.entity.HealthDailyCheckinEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthDailyCheckinRepository
        extends JpaRepository<HealthDailyCheckinEntity, Long> {

    // 查询用户某一天的记录
    Optional<HealthDailyCheckinEntity> findByUserIdAndRecordDate(
            Long userId,
            LocalDate recordDate
    );

    // 按日期范围查询，方便以后计算7天和30天统计
    List<HealthDailyCheckinEntity>
    findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    // 修改记录前进行所属用户校验
    Optional<HealthDailyCheckinEntity> findByIdAndUserId(
            Long id,
            Long userId
    );
}
