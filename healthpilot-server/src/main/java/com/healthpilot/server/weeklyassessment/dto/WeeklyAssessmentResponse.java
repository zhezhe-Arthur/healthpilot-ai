
package com.healthpilot.server.weeklyassessment.dto;

import com.healthpilot.server.weeklyassessment.entity.HealthProfileSnapshotEntity;
import com.healthpilot.server.weeklyassessment.entity.WeeklyFeedbackEntity;

import java.time.LocalDate;

public record WeeklyAssessmentResponse(

        LocalDate weekStartDate,
        LocalDate weekEndDate,

        WeeklyFeedbackResponse feedback,
        HealthProfileSnapshotResponse healthSnapshot

) {

    public static WeeklyAssessmentResponse from(
            WeeklyFeedbackEntity feedback,
            HealthProfileSnapshotEntity snapshot
    ) {
        LocalDate weekStartDate = feedback.getWeekStartDate();

        return new WeeklyAssessmentResponse(
                weekStartDate,
                weekStartDate.plusDays(6),
                WeeklyFeedbackResponse.from(feedback),
                HealthProfileSnapshotResponse.from(snapshot)
        );
    }
}
