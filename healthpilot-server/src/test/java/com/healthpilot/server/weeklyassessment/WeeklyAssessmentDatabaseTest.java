
package com.healthpilot.server.weeklyassessment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WeeklyAssessmentDatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToTestDatabase() {

        // 确认当前连接的是测试数据库
        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        assertEquals("healthpilot_test_db", database);

        // 确认 Flyway V5 执行成功
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '5'
                  AND success = 1
                """,
                Integer.class
        );

        assertEquals(1, migrationCount);

        // 确认两张每周评估表已创建
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                    'health_weekly_feedbacks',
                    'health_profile_snapshots'
                  )
                """,
                Integer.class
        );

        assertEquals(2, tableCount);
    }
}
