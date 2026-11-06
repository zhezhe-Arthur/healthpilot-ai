
package com.healthpilot.server.auth.security;

import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class LoginGuardService {

    public enum Status {
        SUCCESS,
        INVALID_CREDENTIALS,
        RATE_LIMITED
    }

    public record Result(
            Status status,
            Long userId,
            long retryAfterSeconds
    ) {
        public static Result success(Long userId) {
            return new Result(Status.SUCCESS, userId, 0);
        }

        public static Result invalid() {
            return new Result(Status.INVALID_CREDENTIALS, null, 0);
        }

        public static Result limited(long seconds) {
            return new Result(Status.RATE_LIMITED, null, seconds);
        }
    }

    private record Counter(
            LocalDateTime windowStart,
            int attempts,
            LocalDateTime blockedUntil
    ) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final byte[] hmacKey;
    private final String dummyPasswordHash;

    private final int ipMaxAttempts;
    private final int ipWindowSeconds;
    private final int accountMaxFailures;
    private final int accountWindowSeconds;
    private final int lockSeconds;

    public LoginGuardService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${security.login-guard.key-base64}")
            String keyBase64,

            @Value("${security.login-guard.ip-max-attempts:30}")
            int ipMaxAttempts,

            @Value("${security.login-guard.ip-window-seconds:60}")
            int ipWindowSeconds,

            @Value("${security.login-guard.account-max-failures:5}")
            int accountMaxFailures,

            @Value("${security.login-guard.account-window-seconds:900}")
            int accountWindowSeconds,

            @Value("${security.login-guard.lock-seconds:900}")
            int lockSeconds
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        this.hmacKey = Base64.getDecoder().decode(keyBase64);

        if (hmacKey.length < 32) {
            throw new IllegalArgumentException(
                    "登录安全HMAC密钥至少需要32字节"
            );
        }

        if (ipMaxAttempts <= 0
                || ipWindowSeconds <= 0
                || accountMaxFailures <= 0
                || accountWindowSeconds <= 0
                || lockSeconds <= 0) {
            throw new IllegalArgumentException(
                    "登录安全参数必须大于0"
            );
        }

        this.ipMaxAttempts = ipMaxAttempts;
        this.ipWindowSeconds = ipWindowSeconds;
        this.accountMaxFailures = accountMaxFailures;
        this.accountWindowSeconds = accountWindowSeconds;
        this.lockSeconds = lockSeconds;

        // 未注册账号也执行一次BCrypt校验，减小响应时间差异
        this.dummyPasswordHash = passwordEncoder.encode(
                UUID.randomUUID().toString()
        );
    }

    /**
     * 一次登录尝试的完整安全检查。
     *
     * 数据库行锁保证同一账号或IP的并发计数不会丢失。
     * 正常的密码失败和限流结果通过Result返回，
     * 让本事务提交后再由AuthService转换为HTTP异常。
     */
    @Transactional
    public Result authenticate(
            String rawEmail,
            String password,
            String remoteIp
    ) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String ip = remoteIp == null || remoteIp.isBlank()
                ? "unknown"
                : remoteIp;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 不在数据库中保存明文邮箱和IP
        String emailHash = fingerprint("email:" + email);
        String ipHash = fingerprint("ip:" + ip);

        String emailKey = "EM:" + emailHash;
        String ipKey = "IP:" + ipHash;

        // 固定加锁顺序：先邮箱，后IP，降低死锁风险
        Counter emailCounter = lockCounter(emailKey, now);
        Counter ipCounter = lockCounter(ipKey, now);

        // 第一步：检查IP频率
        boolean ipWindowExpired = !now.isBefore(
                ipCounter.windowStart().plusSeconds(ipWindowSeconds)
        );

        LocalDateTime ipWindowStart = ipWindowExpired
                ? now
                : ipCounter.windowStart();

        int ipAttempts = ipWindowExpired
                ? 0
                : ipCounter.attempts();

        if (ipAttempts >= ipMaxAttempts) {
            audit(null, emailHash, ipHash, "IP_LIMITED", now);

            return Result.limited(
                    secondsUntil(
                            ipWindowStart.plusSeconds(ipWindowSeconds),
                            now
                    )
            );
        }

        // 所有进入接口的登录尝试都计入IP窗口
        updateCounter(
                ipKey,
                ipWindowStart,
                ipAttempts + 1,
                null
        );

        // 第二步：检查账号是否还在临时锁定期
        if (emailCounter.blockedUntil() != null
                && now.isBefore(emailCounter.blockedUntil())) {

            audit(
                    null,
                    emailHash,
                    ipHash,
                    "ACCOUNT_LIMITED",
                    now
            );

            return Result.limited(
                    secondsUntil(emailCounter.blockedUntil(), now)
            );
        }

        // 账号时间窗口过期，或旧锁定已经到期，重新计数
        boolean accountWindowExpired = !now.isBefore(
                emailCounter.windowStart()
                        .plusSeconds(accountWindowSeconds)
        );

        boolean lockExpired = emailCounter.blockedUntil() != null;

        LocalDateTime accountWindowStart =
                accountWindowExpired || lockExpired
                        ? now
                        : emailCounter.windowStart();

        int accountFailures =
                accountWindowExpired || lockExpired
                        ? 0
                        : emailCounter.attempts();

        // 第三步：查询用户并验证密码
        UserEntity user = userRepository.findByEmail(email)
                .orElse(null);

        String storedHash = user == null
                ? dummyPasswordHash
                : user.getPasswordHash();

        boolean passwordMatched = passwordEncoder.matches(
                password,
                storedHash
        );

        if (user == null || !passwordMatched) {

            int failures = accountFailures + 1;

            LocalDateTime blockedUntil =
                    failures >= accountMaxFailures
                            ? now.plusSeconds(lockSeconds)
                            : null;

            updateCounter(
                    emailKey,
                    accountWindowStart,
                    failures,
                    blockedUntil
            );

            audit(
                    user == null ? null : user.getId(),
                    emailHash,
                    ipHash,
                    blockedUntil == null
                            ? "LOGIN_FAILURE"
                            : "ACCOUNT_LOCKED",
                    now
            );

            return Result.invalid();
        }

        // 第四步：成功登录，清除该账号的失败计数
        updateCounter(
                emailKey,
                now,
                0,
                null
        );

        audit(
                user.getId(),
                emailHash,
                ipHash,
                "LOGIN_SUCCESS",
                now
        );

        return Result.success(user.getId());
    }

    /**
     * 初始化计数器并取得MySQL排他行锁。
     */
    private Counter lockCounter(
            String key,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO auth_login_limits
                    (limit_key, window_started_at, attempts, blocked_until)
                VALUES (?, ?, 0, NULL)
                """,
                key,
                Timestamp.valueOf(now)
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT window_started_at, attempts, blocked_until
                FROM auth_login_limits
                WHERE limit_key = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> {
                    Timestamp blocked = rs.getTimestamp("blocked_until");

                    return new Counter(
                            rs.getTimestamp("window_started_at")
                                    .toLocalDateTime(),
                            rs.getInt("attempts"),
                            blocked == null
                                    ? null
                                    : blocked.toLocalDateTime()
                    );
                },
                key
        );
    }

    /**
     * 更新计数器，必须在持有对应行锁的事务中调用。
     */
    private void updateCounter(
            String key,
            LocalDateTime windowStart,
            int attempts,
            LocalDateTime blockedUntil
    ) {
        jdbcTemplate.update(
                """
                UPDATE auth_login_limits
                SET window_started_at = ?,
                    attempts = ?,
                    blocked_until = ?
                WHERE limit_key = ?
                """,
                Timestamp.valueOf(windowStart),
                attempts,
                blockedUntil == null
                        ? null
                        : Timestamp.valueOf(blockedUntil),
                key
        );
    }

    /**
     * 保存安全审计事件，不记录密码或真实Token。
     */
    private void audit(
            Long userId,
            String emailHash,
            String ipHash,
            String eventType,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_login_audit
                    (user_id, email_hash, ip_hash, event_type, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                emailHash,
                ipHash,
                eventType,
                Timestamp.valueOf(now)
        );
    }

    /**
     * 用独立密钥生成HMAC-SHA256标识。
     */
    private String fingerprint(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(hmacKey, "HmacSHA256")
            );

            byte[] digest = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(digest);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "登录安全摘要计算失败",
                    e
            );
        }
    }

    private long secondsUntil(
            LocalDateTime end,
            LocalDateTime now
    ) {
        long seconds = Duration.between(now, end).toSeconds();
        return Math.max(1, seconds + 1);
    }
}
