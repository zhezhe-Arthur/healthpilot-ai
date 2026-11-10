
package com.healthpilot.server.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "healthpilot-ai";

    // Access Token验证成功后解析出的身份信息
    public record AccessIdentity(
            Long userId,
            String sessionId
    ) {
    }

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${jwt.secret-base64}") String secretBase64,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        byte[] keyBytes = Base64.getDecoder()
                .decode(secretBase64);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT签名密钥长度不能少于32字节"
            );
        }

        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "JWT有效期必须大于0"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * 根据用户ID和会话ID签发Access Token。
     */
    public String generateToken(
            Long userId,
            String sessionId
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .claim("sid", sessionId)
                .claim("token_type", "access")
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(now.plusSeconds(expirationSeconds))
                )
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 验证签名、签发者、Token类型和有效期。
     * 返回用户ID与关联的会话ID。
     */
    public AccessIdentity parseAccessToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .require("token_type", "access")
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        String sessionId = claims.get("sid", String.class);

        if (userId <= 0 || sessionId == null) {
            throw new IllegalArgumentException("JWT身份信息不合法");
        }

        // 要求会话ID采用标准UUID格式
        UUID uuid = UUID.fromString(sessionId);

        if (!uuid.toString().equals(sessionId)) {
            throw new IllegalArgumentException("JWT会话ID不合法");
        }

        return new AccessIdentity(userId, sessionId);
    }

    // 保留此方法，兼容现有登录测试
    public Long getUserIdFromToken(String token) {
        return parseAccessToken(token).userId();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
