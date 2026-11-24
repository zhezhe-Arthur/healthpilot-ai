
package com.healthpilot.server.weeklyassessment.dto;

import com.healthpilot.server.weeklyassessment.entity.WeeklyFeedbackEntity;

import java.time.LocalDateTime;

public record WeeklyFeedbackResponse(

        Long id,
        String execution,
        String sleep,
        String feeling,
        String note,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static WeeklyFeedbackResponse from(
            WeeklyFeedbackEntity entity
    ) {
        return new WeeklyFeedbackResponse(
                entity.getId(),
                entity.getExecution(),
                entity.getSleep(),
                entity.getFeeling(),
                entity.getNote(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
