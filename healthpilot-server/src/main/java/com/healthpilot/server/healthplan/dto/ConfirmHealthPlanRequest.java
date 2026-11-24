
package com.healthpilot.server.healthplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmHealthPlanRequest(

        @NotNull
        @Min(0)
        Long version
) {
}
