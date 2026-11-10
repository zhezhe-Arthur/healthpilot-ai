
package com.healthpilot.server.auth;

import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.auth.security.SessionService;
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

    @Autowired
    private SessionService sessionService;

    @Value("${jwt.secret-base64}")
    private String testSecretBase64;

    private UserEntity createUser() {

        return userRepository.saveAndFlush(
                new UserEntity(
                        "security-" + UUID.randomUUID() + "@example.com",
                        passwordEncoder.encode("Example123!")
                )
        );
    }

    private String createAccessToken(UserEntity user) {

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        return jwtService.generateToken(
                user.getId(),
                sessionId
        );
    }

    @Test
    void shouldRejectWithoutToken() throws Exception {

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid.jwt.token"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {

        UserEntity user = createUser();
        String token = createAccessToken(user);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(
                        jsonPath("$.data.passwordHash").doesNotExist()
                );
    }

    @Test
    void shouldRejectTokenForUnknownUser() throws Exception {

        UserEntity user = createUser();

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        // 会话属于测试用户，但JWT声称是其他用户
        String token = jwtService.generateToken(
                Long.MAX_VALUE,
                sessionId
        );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {

        UserEntity user = createUser();

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(testSecretBase64)
        );

        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("healthpilot-ai")
                .claim("sid", sessionId)
                .claim("token_type", "access")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(1800)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectForgedToken() throws Exception {

        UserEntity user = createUser();

        String sessionId = sessionService
                .createSession(user.getId())
                .sessionId();

        SecretKey wrongKey =
                Keys.hmacShaKeyFor(new byte[32]);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("healthpilot-ai")
                .claim("sid", sessionId)
                .claim("token_type", "access")
                .expiration(
                        Date.from(Instant.now().plusSeconds(3600))
                )
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLegacyTokenWithoutSessionId() throws Exception {

        UserEntity user = createUser();

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(testSecretBase64)
        );

        // 模拟03-2版本签发的旧Token
        String legacyToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("healthpilot-ai")
                .expiration(
                        Date.from(Instant.now().plusSeconds(3600))
                )
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + legacyToken
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}
