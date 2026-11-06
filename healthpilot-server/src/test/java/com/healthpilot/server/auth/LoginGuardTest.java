
package com.healthpilot.server.auth;

import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createUser() {

        String email = "guard-"
                + UUID.randomUUID()
                + "@example.com";

        userRepository.saveAndFlush(
                new UserEntity(
                        email,
                        passwordEncoder.encode("Example123!")
                )
        );

        return email;
    }

    // 模拟来自指定IP的登录请求
    private ResultActions login(
            String email,
            String password,
            String ip
    ) throws Exception {

        String json = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        );
    }

    // 正确密码可以登录
    @Test
    void shouldLoginSuccessfully() throws Exception {

        String email = createUser();

        login(email, "Example123!", "192.0.2.10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(
                        jsonPath("$.data.accessToken").isString()
                );
    }

    // 密码错误返回401
    @Test
    void shouldRejectWrongPassword() throws Exception {

        String email = createUser();

        login(email, "WrongPassword", "192.0.2.11")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 同一邮箱连续失败5次后被临时锁定
    @Test
    void shouldLockAccountAfterFiveFailures() throws Exception {

        String email = createUser();

        for (int i = 0; i < 5; i++) {
            login(
                    email,
                    "WrongPassword",
                    "192.0.2." + (30 + i)
            )
                    .andExpect(status().isUnauthorized());
        }

        // 即使第6次输入正确密码，也不能立即登录
        login(email, "Example123!", "192.0.2.40")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        Integer lockedEvents = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_login_audit
                WHERE event_type = 'ACCOUNT_LOCKED'
                """,
                Integer.class
        );

        assertEquals(1, lockedEvents);
    }

    // 单个IP超过8次登录尝试时触发限流
    // 正式环境上限仍是30次
    @Test
    void shouldLimitRequestsFromSameIp() throws Exception {

        String ip = "198.51.100.20";

        for (int i = 0; i < 8; i++) {

            String email = "unknown-"
                    + UUID.randomUUID()
                    + "@example.com";

            login(email, "WrongPassword", ip)
                    .andExpect(status().isUnauthorized());
        }

        login(
                "another@example.com",
                "WrongPassword",
                ip
        )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }

    // 锁定到期后，正确密码可以再次登录
    @Test
    void shouldAllowLoginAfterLockExpires() throws Exception {

        String email = createUser();

        for (int i = 0; i < 5; i++) {
            login(
                    email,
                    "WrongPassword",
                    "203.0.113." + (50 + i)
            )
                    .andExpect(status().isUnauthorized());
        }

        // 模拟数据库中的锁定时间已经过去
        LocalDateTime past =
                LocalDateTime.now(ZoneOffset.UTC)
                        .minusMinutes(16);

        jdbcTemplate.update(
                """
                UPDATE auth_login_limits
                SET blocked_until = ?,
                    window_started_at = ?
                WHERE limit_key LIKE 'EM:%'
                """,
                Timestamp.valueOf(past),
                Timestamp.valueOf(past)
        );

        login(email, "Example123!", "203.0.113.80")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // 登录成功时写入审计记录
    @Test
    void shouldRecordSuccessfulLogin() throws Exception {

        String email = createUser();

        login(email, "Example123!", "192.0.2.99")
                .andExpect(status().isOk());

        Integer successEvents = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_login_audit
                WHERE event_type = 'LOGIN_SUCCESS'
                """,
                Integer.class
        );

        assertEquals(1, successEvents);
    }
}
