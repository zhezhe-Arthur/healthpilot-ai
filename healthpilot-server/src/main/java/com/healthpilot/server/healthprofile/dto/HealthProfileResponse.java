
package com.healthpilot.server.healthprofile.dto;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity;
import com.healthpilot.server.healthprofile.entity.HealthProfileEntity.ActivityLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileResponse(
        Long id,
        Short birthYear,
        BigDecimal heightCm,
        ActivityLevel activityLevel,
        String sleepSchedule,
        String healthNotes,
        String timezone,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 将数据库实体转换为对外响应DTO
    public static HealthProfileResponse from(
            HealthProfileEntity entity
    ) {
        return new HealthProfileResponse(
                entity.getId(),
                entity.getBirthYear(),
                entity.getHeightCm(),
                entity.getActivityLevel(),
                entity.getSleepSchedule(),
                entity.getHealthNotes(),
                entity.getTimezone(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
