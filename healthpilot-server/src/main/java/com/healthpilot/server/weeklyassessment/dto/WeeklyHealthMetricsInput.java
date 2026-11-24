
package com.healthpilot.server.weeklyassessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record WeeklyHealthMetricsInput(

        @DecimalMin("0.01")
        @Digits(integer = 4, fraction = 2)
        BigDecimal weightKg,

        @Min(0)
        @Max(1440)
        Short avgSleepMinutes,

        @Min(1)
        @Max(5)
        Byte sleepQuality,

        @Min(1)
        @Max(5)
        Byte moodScore,

        @Min(1)
        @Max(5)
        Byte stressScore,

        @Min(1)
        @Max(5)
        Byte dietQuality,

        @Min(0)
        @Max(10080)
        Integer activityMinutes

) {
}
