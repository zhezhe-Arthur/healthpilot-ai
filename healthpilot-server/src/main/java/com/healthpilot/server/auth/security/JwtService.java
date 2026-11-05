
package com.healthpilot.server.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final String ISSUER = "healthpilot-ai";

    private final SecretKey signingKey;
    private final long expirationSeconds;

    // Spring 从配置文件中读取 JWT 密钥和有效期
    public JwtService(
            @Value("${jwt.secret-base64}") String secretBase64,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        // Base64 字符串转换为真正的密钥字节
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);

        // HS256 要求密钥至少 32 字节
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

    // 根据用户ID签发JWT
    public String generateToken(Long userId) {

        Instant now = Instant.now();

        return Jwts.builder()
                // sub：Token 对应的用户ID
                .subject(userId.toString())

                // iss：签发者
                .issuer(ISSUER)

                // iat：签发时间
                .issuedAt(Date.from(now))

                // exp：过期时间
                .expiration(
                        Date.from(now.plusSeconds(expirationSeconds))
                )

                // 使用 HS256 和密钥签名
                .signWith(signingKey, Jwts.SIG.HS256)

                // 生成最终JWT字符串
                .compact();
    }

    // 验证签名、过期时间和签发者，并读取用户ID
    public Long getUserIdFromToken(String token) {

        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.parseLong(subject);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}

