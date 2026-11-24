
package com.healthpilot.server.healthprofile.dto;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity.ActivityLevel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record UpdateHealthProfileRequest(

        // 前端必须提交读取画像时获得的版本号
        @NotNull(message = "版本号不能为空")
        @PositiveOrZero(message = "版本号不能为负数")
        Long version,

        @Min(1900)
        @Max(2100)
        Short birthYear,

        @DecimalMin("30")
        @DecimalMax("300")
        @Digits(integer = 3, fraction = 2)
        BigDecimal heightCm,

        ActivityLevel activityLevel,

        @Size(max = 100)
        String sleepSchedule,

        @Size(max = 1000)
        String healthNotes,

        @Size(min = 1, max = 64)
        String timezone,

        // 指定要清空的可选字段
        @Size(max = 5)
        List<
                @Pattern(
                        regexp = "birthYear|heightCm|activityLevel|sleepSchedule|healthNotes",
                        message = "不支持清空该字段"
                )
                        String
                > clearFields

) {
}
