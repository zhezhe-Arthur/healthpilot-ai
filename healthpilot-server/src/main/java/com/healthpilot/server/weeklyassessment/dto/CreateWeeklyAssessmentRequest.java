
package com.healthpilot.server.weeklyassessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWeeklyAssessmentRequest(

        @NotNull
        LocalDate weekStartDate,

        @NotNull
        @Valid
        WeeklyFeedbackInput feedback,

        @Valid
        WeeklyHealthMetricsInput healthSnapshot

) {
}
