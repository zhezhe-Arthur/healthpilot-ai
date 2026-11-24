
package com.healthpilot.server.healthplan.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record HealthPlanItemInput(

        @NotBlank
        @Pattern(regexp = "BODY|MENTAL|DIET|SLEEP|GOAL")
        String category,

        @NotBlank
        @Size(max = 150)
        String title,

        @Size(max = 500)
        String description,

        // 1=周一，7=周日，例如：1,3,5
        @NotBlank
        @Pattern(regexp = "^[1-7](,[1-7]){0,6}$")
        String scheduledWeekdays,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 6, fraction = 2)
        BigDecimal targetQuantity,

        @NotBlank
        @Size(max = 20)
        String targetUnit,

        @NotNull
        @Min(0)
        Integer sortOrder
) {
}
