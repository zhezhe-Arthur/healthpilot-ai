
package com.healthpilot.server.auth;

import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 验证注册成功以及密码哈希
    @Test
    void shouldRegisterUser() throws Exception {

        String email = "test-" + UUID.randomUUID() + "@example.com";
        String password = "Example123!";

        String json = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow();

        assertNotEquals(password, user.getPasswordHash());

        assertTrue(passwordEncoder.matches(
                password,
                user.getPasswordHash()
        ));
    }

    // 验证重复邮箱不能注册
    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        userRepository.saveAndFlush(
                new UserEntity(
                        email,
                        passwordEncoder.encode("Example123!")
                )
        );

        String json = """
                {
                    "email": "%s",
                    "password": "Example123!"
                }
                """.formatted(email);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    // 验证密码长度不符合要求
    @Test
    void shouldRejectInvalidPassword() throws Exception {

        String json = """
                {
                    "email": "invalid@example.com",
                    "password": "123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
