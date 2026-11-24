
package com.healthpilot.server.weeklyassessment.dto;

import com.healthpilot.server.weeklyassessment.entity.HealthProfileSnapshotEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileSnapshotResponse(

        Long id,

        Short birthYear,
        BigDecimal heightCm,
        String activityLevel,
        String sleepSchedule,
        String healthNotes,
        String timezone,

        BigDecimal weightKg,
        Short avgSleepMinutes,
        Byte sleepQuality,
        Byte moodScore,
        Byte stressScore,
        Byte dietQuality,
        Integer activityMinutes,

        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static HealthProfileSnapshotResponse from(
            HealthProfileSnapshotEntity entity
    ) {
        return new HealthProfileSnapshotResponse(
                entity.getId(),

                entity.getBirthYear(),
                entity.getHeightCm(),
                entity.getActivityLevel(),
                entity.getSleepSchedule(),
                entity.getHealthNotes(),
                entity.getTimezone(),

                entity.getWeightKg(),
                entity.getAvgSleepMinutes(),
                entity.getSleepQuality(),
                entity.getMoodScore(),
                entity.getStressScore(),
                entity.getDietQuality(),
                entity.getActivityMinutes(),

                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
