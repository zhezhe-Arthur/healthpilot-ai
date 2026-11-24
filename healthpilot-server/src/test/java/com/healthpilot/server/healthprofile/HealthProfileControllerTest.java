
package com.healthpilot.server.healthprofile;

import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.auth.security.SessionService;
import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

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
class HealthProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtService jwtService;

    private UserEntity createUser() {
        return userRepository.saveAndFlush(
                new UserEntity(
                        "profile-" + UUID.randomUUID() + "@example.com",
                        "test-only-password-hash"
                )
        );
    }

    // 使用模块03创建真实的登录会话及JWT
    private String createAccessToken(UserEntity user) {

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        return jwtService.generateToken(
                user.getId(),
                sessionId
        );
    }

    private String validProfileJson() {
        return """
                {
                    "birthYear": 1990,
                    "heightCm": 175.50,
                    "activityLevel": "MODERATE",
                    "sleepSchedule": "23:00-07:00",
                    "healthNotes": "希望改善睡眠",
                    "timezone": "Asia/Shanghai"
                }
                """;
    }

    @Test
    void shouldCreateHealthProfile() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                        post("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validProfileJson())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(
                        jsonPath("$.data.heightCm").value(175.50)
                );
    }

    @Test
    void shouldGetMyHealthProfile() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                post("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileJson())
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.activityLevel")
                                .value("MODERATE")
                )
                .andExpect(
                        jsonPath("$.data.timezone")
                                .value("Asia/Shanghai")
                );
    }

    @Test
    void shouldRejectDuplicateProfile() throws Exception {

        String token = createAccessToken(createUser());

        for (int i = 0; i < 2; i++) {

            mockMvc.perform(
                            post("/api/v1/health-profile")
                                    .header(
                                            HttpHeaders.AUTHORIZATION,
                                            "Bearer " + token
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(validProfileJson())
                    )
                    .andExpect(
                            i == 0
                                    ? status().isCreated()
                                    : status().isConflict()
                    );
        }
    }

    @Test
    void shouldUpdateProfileAndIncreaseVersion()
            throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                post("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileJson())
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        patch("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "version": 0,
                                            "heightCm": 176.20
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(
                        jsonPath("$.data.heightCm").value(176.20)
                )
                .andExpect(
                        jsonPath("$.data.sleepSchedule")
                                .value("23:00-07:00")
                );
    }

    @Test
    void shouldRejectOldVersion() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                post("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileJson())
        ).andExpect(status().isCreated());

        // 第一次修改，version从0更新为1
        mockMvc.perform(
                patch("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"version":0,"heightCm":176}
                                """
                        )
        ).andExpect(status().isOk());

        // 第二次仍携带旧版本号0，应该拒绝
        mockMvc.perform(
                        patch("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":0,"heightCm":177}
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void shouldClearOptionalField() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                post("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileJson())
        ).andExpect(status().isCreated());

        mockMvc.perform(
                        patch("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "version": 0,
                                            "clearFields": ["healthNotes"]
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.healthNotes").doesNotExist()
                );
    }

    @Test
    void shouldIsolateProfilesByUser() throws Exception {

        String tokenA = createAccessToken(createUser());
        String tokenB = createAccessToken(createUser());

        // 用户A建立画像
        mockMvc.perform(
                post("/api/v1/health-profile")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenA
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProfileJson())
        ).andExpect(status().isCreated());

        // 用户B尚未建档，不能读取用户A的画像
        mockMvc.perform(
                        get("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                )
                .andExpect(status().isNotFound());

        // 用户B也不能修改用户A的画像
        mockMvc.perform(
                        patch("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"version":0,"heightCm":180}
                                        """
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidHeight() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                        post("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "heightCm": 400,
                                            "timezone": "Asia/Shanghai"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidTimezone() throws Exception {

        String token = createAccessToken(createUser());

        mockMvc.perform(
                        post("/api/v1/health-profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"timezone":"Invalid/Timezone"}
                                        """
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {

        mockMvc.perform(
                        get("/api/v1/health-profile")
                )
                .andExpect(status().isUnauthorized());
    }
}
