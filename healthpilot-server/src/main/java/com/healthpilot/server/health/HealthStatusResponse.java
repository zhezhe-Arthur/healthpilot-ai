
package com.healthpilot.server.health;

import java.time.Instant;

// 健康检查接口的数据传输对象
public record HealthStatusResponse(
        String service,
        String status,
        Instant timestamp
) {
}
