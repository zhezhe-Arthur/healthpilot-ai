
package com.healthpilot.server.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.time.Duration;

@Service
public class SessionService {

    public record SessionTokens(
            String sessionId,
            String refreshToken
    ) {
    }

    public enum RefreshStatus {
        SUCCESS,
        INVALID,
        REPLAY
    }

    public record RefreshResult(
            RefreshStatus status,
            Long userId,
            String sessionId,
            String refreshToken
    ) {
        public static RefreshResult success(
                Long userId,
                String sessionId,
                String refreshToken
        ) {
            return new RefreshResult(
                    RefreshStatus.SUCCESS,
                    userId,
                    sessionId,
                    refreshToken
            );
        }

        public static RefreshResult invalid() {
            return new RefreshResult(
                    RefreshStatus.INVALID, null, null, null
            );
        }

        public static RefreshResult replay() {
            return new RefreshResult(
                    RefreshStatus.REPLAY, null, null, null
            );
        }
    }

    private record SessionRow(
            Long userId,
            LocalDateTime expiresAt,
            boolean revoked
    ) {
    }

    private record RefreshTokenRow(boolean used) {
    }

    private record ParsedRefreshToken(
            String sessionId,
            String hash
    ) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshExpirationSeconds;

    public SessionService(
            JdbcTemplate jdbcTemplate,
            @Value("${jwt.refresh-expiration-seconds}")
            long refreshExpirationSeconds
    ) {
        if (refreshExpirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Refresh Token有效期必须大于0"
            );
        }

        this.jdbcTemplate = jdbcTemplate;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    /**
     * 登录成功后创建会话。
     *
     * 数据库只保存Refresh Token的SHA-256摘要，
     * 不保存原始Token。
     */
    @Transactional
    public SessionTokens createSession(Long userId) {

        String sessionId = UUID.randomUUID().toString();

        LocalDateTime now = utcNow();
        LocalDateTime expiresAt =
                now.plusSeconds(refreshExpirationSeconds);

        String refreshToken = generateRefreshToken(sessionId);

        jdbcTemplate.update(
                """
                INSERT INTO auth_sessions
                    (id, user_id, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                sessionId,
                userId,
                Timestamp.valueOf(now),
                Timestamp.valueOf(expiresAt)
        );

        insertRefreshToken(sessionId, refreshToken, now);

        return new SessionTokens(sessionId, refreshToken);
    }

    /**
     * 检查Access Token关联的会话是否有效。
     *
     * 撤销或到期的会话，不再允许访问受保护接口。
     */
    public boolean isSessionActive(
            String sessionId,
            Long userId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_sessions
                WHERE id = ?
                  AND user_id = ?
                  AND revoked_at IS NULL
                  AND expires_at > ?
                """,
                Integer.class,
                sessionId,
                userId,
                Timestamp.valueOf(utcNow())
        );

        return count != null && count == 1;
    }

    /**
     * 原子轮换Refresh Token。
     *
     * 先锁定会话，再检查历史Token，
     * 避免并发刷新得到多个有效Token。
     *
     * 注意：失败时返回Result而不是直接抛异常，
     * 这样检测到重放后的撤销更新才能提交事务。
     */
    @Transactional
    public RefreshResult rotate(String rawToken) {

        ParsedRefreshToken parsed = parseRefreshToken(rawToken);

        if (parsed == null) {
            return RefreshResult.invalid();
        }

        // 锁住当前会话，串行处理此会话的所有刷新请求
        List<SessionRow> sessions = jdbcTemplate.query(
                """
                SELECT user_id, expires_at, revoked_at
                FROM auth_sessions
                WHERE id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new SessionRow(
                        rs.getLong("user_id"),
                        rs.getTimestamp("expires_at")
                                .toLocalDateTime(),
                        rs.getTimestamp("revoked_at") != null
                ),
                parsed.sessionId()
        );

        if (sessions.isEmpty()) {
            return RefreshResult.invalid();
        }


        SessionRow session = sessions.get(0);

        // 已经获得数据库会话锁，重新获取当前时间
        LocalDateTime now = utcNow();

        if (session.revoked()
                || !now.isBefore(session.expiresAt())) {
            return RefreshResult.invalid();
        }


        // 查询提交的Token是否为本会话曾签发的Token
        List<RefreshTokenRow> tokens = jdbcTemplate.query(
                """
                SELECT used_at
                FROM auth_refresh_tokens
                WHERE session_id = ?
                  AND token_hash = ?
                """,
                (rs, rowNum) -> new RefreshTokenRow(
                        rs.getTimestamp("used_at") != null
                ),
                parsed.sessionId(),
                parsed.hash()
        );

        // 随机伪造的Token不应导致用户会话被恶意撤销
        if (tokens.isEmpty()) {
            return RefreshResult.invalid();
        }

        // 已经使用过的Refresh Token再次出现：重放
        if (tokens.get(0).used()) {

            jdbcTemplate.update(
                    """
                    UPDATE auth_sessions
                    SET revoked_at = ?
                    WHERE id = ?
                      AND revoked_at IS NULL
                    """,
                    Timestamp.valueOf(now),
                    parsed.sessionId()
            );

            return RefreshResult.replay();
        }

        // 将旧Refresh Token标记为已使用
        jdbcTemplate.update(
                """
                UPDATE auth_refresh_tokens
                SET used_at = ?
                WHERE session_id = ?
                  AND token_hash = ?
                  AND used_at IS NULL
                """,
                Timestamp.valueOf(now),
                parsed.sessionId(),
                parsed.hash()
        );

        // 生成新的随机Refresh Token
        String newRefreshToken =
                generateRefreshToken(parsed.sessionId());

        insertRefreshToken(
                parsed.sessionId(),
                newRefreshToken,
                now
        );

        jdbcTemplate.update(
                """
                UPDATE auth_sessions
                SET last_used_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(now),
                parsed.sessionId()
        );

        return RefreshResult.success(
                session.userId(),
                parsed.sessionId(),
                newRefreshToken
        );
    }

    /**
     * 退出当前登录会话。
     */
    @Transactional
    public void revokeSession(
            String sessionId,
            Long userId
    ) {
        jdbcTemplate.update(
                """
                UPDATE auth_sessions
                SET revoked_at = ?
                WHERE id = ?
                  AND user_id = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.valueOf(utcNow()),
                sessionId,
                userId
        );
    }

    /**
     * 撤销该用户全部登录会话。
     */
    @Transactional
    public void revokeAllSessions(Long userId) {

        jdbcTemplate.update(
                """
                UPDATE auth_sessions
                SET revoked_at = ?
                WHERE user_id = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.valueOf(utcNow()),
                userId
        );
    }

    /**
     * Refresh Token格式：
     * 会话UUID.32字节随机值的Base64URL
     */
    private String generateRefreshToken(String sessionId) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String secret = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return sessionId + "." + secret;
    }

    private void insertRefreshToken(
            String sessionId,
            String token,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_refresh_tokens
                    (session_id, token_hash, issued_at)
                VALUES (?, ?, ?)
                """,
                sessionId,
                hashToken(token),
                Timestamp.valueOf(now)
        );
    }

    /**
     * 检查格式并得到会话ID及Token摘要。
     */
    private ParsedRefreshToken parseRefreshToken(String token) {

        if (token == null || token.length() > 200) {
            return null;
        }

        int dotIndex = token.indexOf('.');

        if (dotIndex <= 0 || dotIndex == token.length() - 1) {
            return null;
        }

        try {
            String sessionId = token.substring(0, dotIndex);
            String secret = token.substring(dotIndex + 1);

            UUID uuid = UUID.fromString(sessionId);

            // 拒绝非标准UUID文本形式
            if (!uuid.toString().equals(sessionId)) {
                return null;
            }

            byte[] secretBytes = Base64.getUrlDecoder()
                    .decode(secret);

            if (secretBytes.length != 32) {
                return null;
            }

            return new ParsedRefreshToken(
                    sessionId,
                    hashToken(token)
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256算法不可用",
                    e
            );
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }


    /**
     * 查询Refresh Token所属会话的剩余有效期。
     * 刷新不会延长7天的绝对截止时间。
     */
    public long getRemainingSeconds(String sessionId) {

        LocalDateTime expiresAt = jdbcTemplate.queryForObject(
                """
                SELECT expires_at
                FROM auth_sessions
                WHERE id = ?
                """,
                (rs, rowNum) ->
                        rs.getTimestamp("expires_at")
                                .toLocalDateTime(),
                sessionId
        );

        return Math.max(
                0,
                Duration.between(
                        utcNow(),
                        expiresAt
                ).getSeconds()
        );
    }

}
