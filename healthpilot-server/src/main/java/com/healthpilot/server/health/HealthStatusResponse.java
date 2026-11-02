
package com.healthpilot.server.health;

import java.time.Instant;

public record HealthStatusResponse(
        String service,
        String status,
        Instant timestamp
) {
}
