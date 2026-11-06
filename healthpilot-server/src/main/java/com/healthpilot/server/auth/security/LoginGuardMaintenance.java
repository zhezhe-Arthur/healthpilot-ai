
package com.healthpilot.server.auth.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class LoginGuardMaintenance {

    private final JdbcTemplate jdbcTemplate;

    public LoginGuardMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 每天UTC时间03:00清理过期数据
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void cleanup() {

        // 审计记录保留30天
        jdbcTemplate.update(
                """
                DELETE FROM auth_login_audit
                WHERE occurred_at < UTC_TIMESTAMP(6)
                                      - INTERVAL 30 DAY
                """
        );

        // 删除两天前已不再生效的限流计数器
        jdbcTemplate.update(
                """
                DELETE FROM auth_login_limits
                WHERE window_started_at < UTC_TIMESTAMP(6)
                                          - INTERVAL 2 DAY
                  AND (
                      blocked_until IS NULL
                      OR blocked_until < UTC_TIMESTAMP(6)
                  )
                """
        );
    }
}
