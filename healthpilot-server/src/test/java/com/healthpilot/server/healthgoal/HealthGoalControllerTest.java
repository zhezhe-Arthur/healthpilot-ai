
package com.healthpilot.server.healthgoal;

import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.auth.security.SessionService;
import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HealthGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtService jwtService;

    private String createAccessToken() {

        UserEntity user = userRepository.saveAndFlush(
                new UserEntity(
                        "goal-" + UUID.randomUUID() + "@example.com",
                        "test-only-password-hash"
                )
        );

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        return jwtService.generateToken(
                user.getId(),
                sessionId
        );
    }

    private String goalJson() {
        return """
                {
                    "category": "EXERCISE",
                    "title": "每周运动150分钟",
                    "description": "坚持规律运动",
                    "targetValue": 150,
                    "targetUnit": "min/week",
                    "targetDate": "2026-12-31"
                }
                """;
    }

    private long createGoal(String token) throws Exception {
        return createGoal(token, goalJson());
    }

    private long createGoal(
            String token,
            String json
    ) throws Exception {

        String response = mockMvc.perform(
                        post("/api/v1/health-goals")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(
                        jsonPath("$.data.status").value("ACTIVE")
                )
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(
                response,
                "$.data.id"
        )).longValue();
    }

    @Test
    void shouldCreateGoal() throws Exception {
        String token = createAccessToken();

        long id = createGoal(token);

        assert id > 0;
    }

    @Test
    void shouldGetGoalById() throws Exception {
        String token = createAccessToken();
        long id = createGoal(token);

        mockMvc.perform(
                        get("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.title")
                                .value("每周运动150分钟")
                )
                .andExpect(
                        jsonPath("$.data.targetValue")
                                .value(150)
                );
    }

    @Test
    void shouldFilterGoalsByStatus() throws Exception {
        String token = createAccessToken();

        createGoal(token);
        long secondId = createGoal(token);

        // 暂停其中一个目标
        mockMvc.perform(
                patch("/api/v1/health-goals/{id}", secondId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"version":0,"status":"PAUSED"}
                                """
                        )
        ).andExpect(status().isOk());

        // 全部目标有两条
        mockMvc.perform(
                        get("/api/v1/health-goals")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()").value(2)
                );

        // 进行中的目标只有一条
        mockMvc.perform(
                        get("/api/v1/health-goals")
                                .param("status", "ACTIVE")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()").value(1)
                )
                .andExpect(
                        jsonPath("$.data[0].status")
                                .value("ACTIVE")
                );
    }

    @Test
    void shouldUpdateGoalAndVersion() throws Exception {
        String token = createAccessToken();
        long id = createGoal(token);

        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "version": 0,
                                            "title": "每周运动180分钟",
                                            "targetValue": 180
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.version").value(1)
                )
                .andExpect(
                        jsonPath("$.data.targetValue").value(180)
                )
                .andExpect(
                        jsonPath("$.data.targetUnit")
                                .value("min/week")
                );
    }

    @Test
    void shouldRejectStaleVersion() throws Exception {
        String token = createAccessToken();
        long id = createGoal(token);

        mockMvc.perform(
                patch("/api/v1/health-goals/{id}", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"version":0,"status":"PAUSED"}
                                """
                        )
        ).andExpect(status().isOk());

        // version已经变为1，旧版本不能再次修改
        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":0,"status":"ACTIVE"}
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void shouldRejectChangesAfterCompletion() throws Exception {
        String token = createAccessToken();
        long id = createGoal(token);

        // 完成目标
        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":0,"status":"COMPLETED"}
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("COMPLETED")
                );

        // 已完成目标不能恢复为进行中
        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":1,"status":"ACTIVE"}
                                        """
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldClearTargetValueAndUnit() throws Exception {
        String token = createAccessToken();
        long id = createGoal(token);

        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "version": 0,
                                            "clearFields": ["target"]
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.targetValue")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.targetUnit")
                                .doesNotExist()
                );
    }

    @Test
    void shouldIsolateGoalsByUser() throws Exception {
        String tokenA = createAccessToken();
        String tokenB = createAccessToken();

        long id = createGoal(tokenA);

        // 用户B不能查看用户A的目标
        mockMvc.perform(
                        get("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                )
                .andExpect(status().isNotFound());

        // 用户B也不能修改
        mockMvc.perform(
                        patch("/api/v1/health-goals/{id}", id)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":0,"status":"PAUSED"}
                                        """
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectIncompleteTarget() throws Exception {
        String token = createAccessToken();

        mockMvc.perform(
                        post("/api/v1/health-goals")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "category": "BODY",
                                            "title": "控制体重",
                                            "targetValue": 70
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/v1/health-goals")
                )
                .andExpect(status().isUnauthorized());
    }
}
