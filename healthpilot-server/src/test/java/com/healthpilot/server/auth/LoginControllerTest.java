
package com.healthpilot.server.auth;

import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // 创建测试用户
    private UserEntity createTestUser(String email, String password) {

        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(password)
        );

        return userRepository.saveAndFlush(user);
    }

    // 正确密码登录，验证JWT
    @Test
    void shouldLoginSuccessfully() throws Exception {

        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "Example123!";

        UserEntity user = createTestUser(email, password);

        String json = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        String responseBody = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从登录响应中读取JWT
        String token = JsonPath.read(
                responseBody,
                "$.data.accessToken"
        );

        // 验证签名并读取JWT里的用户ID
        Long userId = jwtService.getUserIdFromToken(token);

        assertEquals(user.getId(), userId);
    }

    // 错误密码登录
    @Test
    void shouldRejectWrongPassword() throws Exception {

        String email = "login-" + UUID.randomUUID() + "@example.com";

        createTestUser(email, "Example123!");

        String json = """
                {
                    "email": "%s",
                    "password": "WrongPassword123"
                }
                """.formatted(email);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // 账号不存在
    @Test
    void shouldRejectUnknownEmail() throws Exception {

        String email = "unknown-" + UUID.randomUUID() + "@example.com";

        String json = """
                {
                    "email": "%s",
                    "password": "Example123!"
                }
                """.formatted(email);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }
}
