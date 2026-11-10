
package com.healthpilot.server.auth;

import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionLifecycleTest {

    private record Tokens(
            String access,
            String refresh
    ) {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity createUser() {

        return userRepository.saveAndFlush(
                new UserEntity(
                        "session-" + UUID.randomUUID() + "@example.com",
                        passwordEncoder.encode("Example123!")
                )
        );
    }

    private Tokens login(UserEntity user) throws Exception {

        String json = """
                {
                    "email": "%s",
                    "password": "Example123!"
                }
                """.formatted(user.getEmail());

        String body = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("192.0.2.200");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(
                        jsonPath("$.data.refreshExpiresIn")
                                .value(604800)
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new Tokens(
                JsonPath.read(body, "$.data.accessToken"),
                JsonPath.read(body, "$.data.refreshToken")
        );
    }

    private Tokens refresh(String refreshToken) throws Exception {

        String json = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

        String body = mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new Tokens(
                JsonPath.read(body, "$.data.accessToken"),
                JsonPath.read(body, "$.data.refreshToken")
        );
    }

    private void expectMeOk(String accessToken) throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk());
    }

    private void expectMe401(String accessToken) throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    private void expectRefresh401(String refreshToken) throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"refreshToken":"%s"}
                                        """.formatted(refreshToken)
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 刷新成功后，新Token可用
    @Test
    void shouldRefreshTokensSuccessfully() throws Exception {

        UserEntity user = createUser();

        Tokens first = login(user);
        Tokens second = refresh(first.refresh());

        expectMeOk(first.access());
        expectMeOk(second.access());

        // 新的Refresh Token也应该可以继续刷新
        Tokens third = refresh(second.refresh());

        expectMeOk(third.access());
    }

    // 旧Refresh Token重放，撤销整个会话
    @Test
    void shouldRevokeSessionOnRefreshReplay() throws Exception {

        UserEntity user = createUser();

        Tokens first = login(user);
        Tokens second = refresh(first.refresh());

        // 再次使用已消费的第一个Refresh Token
        expectRefresh401(first.refresh());

        // 即使新Access Token没有过期，也应该被拒绝
        expectMe401(second.access());

        // 第二个Refresh Token也无法再刷新
        expectRefresh401(second.refresh());
    }

    // 退出当前设备，Access和Refresh同时失效
    @Test
    void shouldLogoutCurrentSession() throws Exception {

        UserEntity user = createUser();
        Tokens tokens = login(user);

        expectMeOk(tokens.access());

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokens.access()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        expectMe401(tokens.access());
        expectRefresh401(tokens.refresh());
    }

    // 全部设备退出需要密码确认
    @Test
    void shouldLogoutAllDevices() throws Exception {

        UserEntity user = createUser();

        Tokens deviceA = login(user);
        Tokens deviceB = login(user);

        mockMvc.perform(
                        post("/api/v1/auth/logout-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + deviceA.access()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"password":"Example123!"}
                                        """
                                )
                )
                .andExpect(status().isOk());

        expectMe401(deviceA.access());
        expectMe401(deviceB.access());

        expectRefresh401(deviceA.refresh());
        expectRefresh401(deviceB.refresh());
    }

    // 错误密码不得撤销全部会话
    @Test
    void shouldRejectLogoutAllWithWrongPassword() throws Exception {

        UserEntity user = createUser();
        Tokens tokens = login(user);

        mockMvc.perform(
                        post("/api/v1/auth/logout-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokens.access()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"password":"WrongPassword123"}
                                        """
                                )
                )
                .andExpect(status().isUnauthorized());

        expectMeOk(tokens.access());
    }

    // 未知Refresh Token应返回401
    @Test
    void shouldRejectUnknownRefreshToken() throws Exception {

        expectRefresh401("not-a-valid-refresh-token");
    }

    // 会话过期后，Access和Refresh都不能继续使用
    @Test
    void shouldRejectExpiredSession() throws Exception {

        UserEntity user = createUser();
        Tokens tokens = login(user);

        jdbcTemplate.update(
                """
                UPDATE auth_sessions
                SET expires_at = ?
                WHERE user_id = ?
                """,
                Timestamp.valueOf(
                        LocalDateTime.now(ZoneOffset.UTC)
                                .minusSeconds(10)
                ),
                user.getId()
        );

        expectMe401(tokens.access());
        expectRefresh401(tokens.refresh());
    }

    // 一个会话退出，不影响同一用户的另一个会话
    @Test
    void shouldOnlyLogoutCurrentDevice() throws Exception {

        UserEntity user = createUser();

        Tokens deviceA = login(user);
        Tokens deviceB = login(user);

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + deviceA.access()
                                )
                )
                .andExpect(status().isOk());

        expectMe401(deviceA.access());
        expectMeOk(deviceB.access());
    }
}
