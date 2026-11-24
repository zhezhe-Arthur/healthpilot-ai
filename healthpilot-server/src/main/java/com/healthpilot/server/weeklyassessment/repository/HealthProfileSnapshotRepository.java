
package com.healthpilot.server.weeklyassessment.repository;

import com.healthpilot.server.weeklyassessment.entity.HealthProfileSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthProfileSnapshotRepository
        extends JpaRepository<HealthProfileSnapshotEntity, Long> {

    // 查询指定周的健康画像快照
    Optional<HealthProfileSnapshotEntity> findByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 判断指定周是否存在快照
    boolean existsByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 查询历史快照，用于AI健康趋势分析
    List<HealthProfileSnapshotEntity>
    findByUserIdAndWeekStartDateBetweenOrderByWeekStartDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
