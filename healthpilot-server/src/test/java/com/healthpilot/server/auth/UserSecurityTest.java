
package com.healthpilot.server.auth;

import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret-base64}")
    private String testSecretBase64;

    // 创建一个测试用户
    private UserEntity createTestUser() {

        String email = "security-"
                + UUID.randomUUID()
                + "@example.com";

        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode("Example123!")
        );

        return userRepository.saveAndFlush(user);
    }

    // 不携带Token，应拒绝访问
    @Test
    void shouldRejectRequestWithoutToken() throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 传入无效Token，应拒绝访问
    @Test
    void shouldRejectInvalidToken() throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid.jwt.token"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 有效Token可以获取当前用户信息
    @Test
    void shouldReturnCurrentUser() throws Exception {

        UserEntity user = createTestUser();

        String token = jwtService.generateToken(user.getId());

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    // Token签名正确，但用户不存在
    @Test
    void shouldRejectTokenForUnknownUser() throws Exception {

        String token = jwtService.generateToken(Long.MAX_VALUE);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 过期Token不能访问接口
    @Test
    void shouldRejectExpiredToken() throws Exception {

        UserEntity user = createTestUser();

        byte[] keyBytes = Base64.getDecoder()
                .decode(testSecretBase64);

        SecretKey signingKey = Keys.hmacShaKeyFor(keyBytes);

        Instant now = Instant.now();

        // 生成一个过期时间在过去的测试Token
        String expiredToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("healthpilot-ai")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(1800)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + expiredToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // 使用其他密钥伪造签名，应拒绝访问
    @Test
    void shouldRejectForgedToken() throws Exception {

        UserEntity user = createTestUser();

        // 仅用于测试：故意使用错误的签名密钥
        SecretKey wrongKey = Keys.hmacShaKeyFor(new byte[32]);

        String forgedToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("healthpilot-ai")
                .expiration(
                        Date.from(Instant.now().plusSeconds(3600))
                )
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + forgedToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
