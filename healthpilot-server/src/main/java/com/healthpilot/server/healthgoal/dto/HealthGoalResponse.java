
package com.healthpilot.server.healthgoal.dto;

import com.healthpilot.server.healthgoal.entity.HealthGoalEntity;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalCategory;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthGoalResponse(
        Long id,
        GoalCategory category,
        String title,
        String description,
        BigDecimal targetValue,
        String targetUnit,
        LocalDate targetDate,
        GoalStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HealthGoalResponse from(
            HealthGoalEntity entity
    ) {
        return new HealthGoalResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTargetValue(),
                entity.getTargetUnit(),
                entity.getTargetDate(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
