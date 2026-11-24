
package com.healthpilot.server.healthplan.dto;

import com.healthpilot.server.healthplan.entity.HealthPlanEntity;
import com.healthpilot.server.healthplan.entity.HealthPlanItemEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HealthPlanResponse(
        Long id,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String status,
        String aiSummary,
        Long version,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<HealthPlanItemResponse> items
) {

    public static HealthPlanResponse from(
            HealthPlanEntity plan,
            List<HealthPlanItemEntity> itemEntities
    ) {
        List<HealthPlanItemResponse> items =
                itemEntities.stream()
                        .map(HealthPlanItemResponse::from)
                        .toList();

        return new HealthPlanResponse(
                plan.getId(),
                plan.getWeekStartDate(),
                plan.getWeekStartDate().plusDays(6),
                plan.getStatus(),
                plan.getAiSummary(),
                plan.getVersion(),
                plan.getConfirmedAt(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                items
        );
    }
}
