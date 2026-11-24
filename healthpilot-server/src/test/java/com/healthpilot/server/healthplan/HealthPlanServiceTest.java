
package com.healthpilot.server.healthplan;

import com.healthpilot.server.healthplan.dto.*;
import com.healthpilot.server.healthplan.exception.HealthPlanBusinessException;
import com.healthpilot.server.healthplan.service.HealthPlanService;
import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HealthPlanServiceTest {

    private static final LocalDate WEEK =
            LocalDate.of(2026, 11, 30);

    @Autowired
    private HealthPlanService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {

        // 确保测试不会操作开发数据库
        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        assertEquals("healthpilot_test_db", database);

        // 每次测试创建独立用户
        String email = "plan-test-"
                + UUID.randomUUID()
                + "@example.com";

        UserEntity user = userRepository.saveAndFlush(
                new UserEntity(email, "test-password-hash")
        );

        userId = user.getId();
    }

    // 测试1：创建、查询、确认计划
    @Test
    void shouldCreateQueryAndConfirmPlan() {

        var draft = service.saveDraft(
                userId,
                validRequest()
        );

        assertEquals("DRAFT", draft.status());
        assertEquals(2, draft.items().size());

        var queried = service.getByWeek(userId, WEEK);

        assertEquals(draft.id(), queried.id());
        assertEquals(2, queried.items().size());

        var confirmed = service.confirm(
                userId,
                draft.id(),
                new ConfirmHealthPlanRequest(draft.version())
        );

        assertEquals("CONFIRMED", confirmed.status());
        assertNotNull(confirmed.confirmedAt());
        assertTrue(confirmed.version() > draft.version());

        // 已确认计划不能再次覆盖
        var exception = assertThrows(
                HealthPlanBusinessException.class,
                () -> service.saveDraft(
                        userId,
                        validRequest()
                )
        );

        assertEquals(
                HealthPlanBusinessException.Reason.PLAN_CONFIRMED,
                exception.getReason()
        );
    }

    // 测试2：重新生成草稿，旧任务应被替换
    @Test
    void shouldReplaceDraftItems() {

        var original = service.saveDraft(
                userId,
                validRequest()
        );

        var replacement = new SaveHealthPlanDraftRequest(
                WEEK,
                "重新生成后的健康计划",
                List.of(
                        new HealthPlanItemInput(
                                "BODY",
                                "每周游泳2次",
                                "每次30分钟",
                                "2,6",
                                new BigDecimal("2"),
                                "次",
                                1
                        )
                )
        );

        var updated = service.saveDraft(
                userId,
                replacement
        );

        assertEquals(original.id(), updated.id());
        assertEquals("DRAFT", updated.status());
        assertEquals(1, updated.items().size());

        var queried = service.getByWeek(userId, WEEK);

        assertEquals(1, queried.items().size());
        assertEquals(
                "每周游泳2次",
                queried.items().get(0).title()
        );
    }

    // 测试3：不能读取或确认其他用户的计划
    @Test
    void shouldIsolatePlansByUser() {

        var draft = service.saveDraft(
                userId,
                validRequest()
        );

        Long anotherUserId = userId + 100000L;

        var readException = assertThrows(
                HealthPlanBusinessException.class,
                () -> service.getByWeek(
                        anotherUserId,
                        WEEK
                )
        );

        assertEquals(
                HealthPlanBusinessException.Reason.NOT_FOUND,
                readException.getReason()
        );

        var confirmException = assertThrows(
                HealthPlanBusinessException.class,
                () -> service.confirm(
                        anotherUserId,
                        draft.id(),
                        new ConfirmHealthPlanRequest(
                                draft.version()
                        )
                )
        );

        assertEquals(
                HealthPlanBusinessException.Reason.NOT_FOUND,
                confirmException.getReason()
        );
    }

    // 测试4：非法日期和错误版本
    @Test
    void shouldRejectInvalidDateAndVersion() {

        // 2026-12-01 是星期二
        var invalidRequest = new SaveHealthPlanDraftRequest(
                LocalDate.of(2026, 12, 1),
                "非法日期测试",
                validItems()
        );

        var dateException = assertThrows(
                HealthPlanBusinessException.class,
                () -> service.saveDraft(
                        userId,
                        invalidRequest
                )
        );

        assertEquals(
                HealthPlanBusinessException.Reason.INVALID_INPUT,
                dateException.getReason()
        );

        var draft = service.saveDraft(
                userId,
                validRequest()
        );

        // 提交与数据库不一致的版本
        var versionException = assertThrows(
                HealthPlanBusinessException.class,
                () -> service.confirm(
                        userId,
                        draft.id(),
                        new ConfirmHealthPlanRequest(
                                draft.version() + 1
                        )
                )
        );

        assertEquals(
                HealthPlanBusinessException.Reason.VERSION_CONFLICT,
                versionException.getReason()
        );
    }

    private SaveHealthPlanDraftRequest validRequest() {
        return new SaveHealthPlanDraftRequest(
                WEEK,
                "本周改善睡眠和运动习惯",
                validItems()
        );
    }

    private List<HealthPlanItemInput> validItems() {
        return List.of(
                new HealthPlanItemInput(
                        "BODY",
                        "快走30分钟",
                        "每周快走3次",
                        "1,3,5",
                        new BigDecimal("3"),
                        "次",
                        1
                ),
                new HealthPlanItemInput(
                        "SLEEP",
                        "每晚睡眠不少于7小时",
                        "保持规律作息",
                        "1,2,3,4,5,6,7",
                        new BigDecimal("7"),
                        "天",
                        2
                )
        );
    }
}
