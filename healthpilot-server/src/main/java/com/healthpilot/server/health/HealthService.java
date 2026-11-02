
package com.healthpilot.server.health;

import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class HealthService {

    public HealthStatusResponse currentStatus() {

        return new HealthStatusResponse(
                "healthpilot-server",
                "UP",
                Instant.now()
        );
    }
}
