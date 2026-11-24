
package com.healthpilot.server.healthprofile.dto;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity.ActivityLevel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateHealthProfileRequest(

        @Min(value = 1900, message = "出生年份不能早于1900年")
        @Max(value = 2100, message = "出生年份不能晚于2100年")
        Short birthYear,

        @DecimalMin(value = "30", message = "身高不能小于30厘米")
        @DecimalMax(value = "300", message = "身高不能大于300厘米")
        @Digits(integer = 3, fraction = 2,
                message = "身高最多保留两位小数")
        BigDecimal heightCm,

        ActivityLevel activityLevel,

        @Size(max = 100, message = "作息描述不能超过100个字符")
        String sleepSchedule,

        @Size(max = 1000, message = "健康备注不能超过1000个字符")
        String healthNotes,

        @NotBlank(message = "时区不能为空")
        @Size(max = 64, message = "时区不能超过64个字符")
        String timezone

) {
}
