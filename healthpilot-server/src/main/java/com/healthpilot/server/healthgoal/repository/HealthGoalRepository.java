
package com.healthpilot.server.healthgoal.repository;

import com.healthpilot.server.healthgoal.entity.HealthGoalEntity;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthGoalRepository
        extends JpaRepository<HealthGoalEntity, Long> {

    // 查询当前用户的全部健康目标
    List<HealthGoalEntity> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // 按状态查询，例如当前进行中的健康目标
    List<HealthGoalEntity> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            GoalStatus status
    );

    // 目标修改和删除前，确认目标属于当前用户
    Optional<HealthGoalEntity> findByIdAndUserId(
            Long id,
            Long userId
    );
}
