
package com.healthpilot.server.healthplan.dto;

import com.healthpilot.server.healthplan.entity.HealthPlanItemEntity;

import java.math.BigDecimal;

public record HealthPlanItemResponse(
        Long id,
        String category,
        String title,
        String description,
        String scheduledWeekdays,
        BigDecimal targetQuantity,
        String targetUnit,
        Integer sortOrder
) {

    public static HealthPlanItemResponse from(
            HealthPlanItemEntity entity
    ) {
        return new HealthPlanItemResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getScheduledWeekdays(),
                entity.getTargetQuantity(),
                entity.getTargetUnit(),
                entity.getSortOrder()
        );
    }
}
