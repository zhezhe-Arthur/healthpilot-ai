
package com.healthpilot.server.weeklyassessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WeeklyFeedbackInput(

        @NotBlank
        @Pattern(regexp = "all|most|few")
        String execution,

        @NotBlank
        @Pattern(regexp = "better|same|worse")
        String sleep,

        @NotBlank
        @Pattern(regexp = "good|normal|bad")
        String feeling,

        @Size(max = 1000)
        String note

) {
}
