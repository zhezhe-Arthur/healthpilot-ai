
package com.healthpilot.server.healthgoal.dto;

import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateHealthGoalRequest(

        @NotNull(message = "目标类别不能为空")
        GoalCategory category,

        @NotBlank(message = "目标名称不能为空")
        @Size(max = 120, message = "目标名称不能超过120个字符")
        String title,

        @Size(max = 1000, message = "目标描述不能超过1000个字符")
        String description,

        @DecimalMin(value = "0", message = "目标值不能为负数")
        @Digits(integer = 8, fraction = 2)
        BigDecimal targetValue,

        @Size(max = 20, message = "目标单位不能超过20个字符")
        String targetUnit,

        LocalDate targetDate
) {
}
