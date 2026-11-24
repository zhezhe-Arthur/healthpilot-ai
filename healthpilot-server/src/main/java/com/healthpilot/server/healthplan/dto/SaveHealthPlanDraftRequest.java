
package com.healthpilot.server.healthplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record SaveHealthPlanDraftRequest(

        @NotNull
        LocalDate weekStartDate,

        @Size(max = 5000)
        String aiSummary,

        @NotEmpty
        @Size(max = 40)
        List<@Valid HealthPlanItemInput> items
) {
}
