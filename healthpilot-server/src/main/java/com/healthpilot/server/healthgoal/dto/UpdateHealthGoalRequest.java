
package com.healthpilot.server.healthgoal.dto;

import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalCategory;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateHealthGoalRequest(

        @NotNull(message = "版本号不能为空")
        @PositiveOrZero
        Long version,

        GoalCategory category,

        @Size(min = 1, max = 120)
        String title,

        @Size(max = 1000)
        String description,

        @DecimalMin("0")
        @Digits(integer = 8, fraction = 2)
        BigDecimal targetValue,

        @Size(min = 1, max = 20)
        String targetUnit,

        LocalDate targetDate,

        GoalStatus status,

        // 可清空的字段：描述、数值目标、截止日期
        @Size(max = 3)
        List<
                @Pattern(
                        regexp = "description|target|targetDate",
                        message = "不支持清空该字段"
                )
                        String
                > clearFields
) {
}
