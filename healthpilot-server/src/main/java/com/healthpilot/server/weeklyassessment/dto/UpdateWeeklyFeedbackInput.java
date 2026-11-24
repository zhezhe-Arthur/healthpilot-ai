
package com.healthpilot.server.weeklyassessment.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWeeklyFeedbackInput(

        @Pattern(regexp = "all|most|few")
        String execution,

        @Pattern(regexp = "better|same|worse")
        String sleep,

        @Pattern(regexp = "good|normal|bad")
        String feeling,

        @Size(max = 1000)
        String note

) {
}
