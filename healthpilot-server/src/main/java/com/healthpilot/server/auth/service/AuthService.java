
package com.healthpilot.server.auth.service;

// 响应和请求对象：接收传来的注册信息，返回用户ID和邮箱
import com.healthpilot.server.auth.dto.*;

// 自定义异常：当邮箱已经被注册时抛出
import com.healthpilot.server.auth.exception.EmailAlreadyExistsException;
import com.healthpilot.server.auth.exception.InvalidRefreshTokenException;
import com.healthpilot.server.auth.exception.LoginRateLimitException;

import com.healthpilot.server.auth.exception.InvalidCredentialsException;
import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.auth.security.JwtService;
// 数据库用户实体类
import com.healthpilot.server.auth.security.LoginGuardService;
import com.healthpilot.server.auth.security.SessionService;
import com.healthpilot.server.user.entity.UserEntity;
// 数据访问接口
import com.healthpilot.server.user.repository.UserRepository;


// 数据库完整性约束异常，例如违反唯一索引
import org.springframework.dao.DataIntegrityViolationException;
// 密码哈希和服务声明
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
// 声明数据库
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

// 用户认证业务服务
// 用户注册能力
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // JWT服务对象：这行很重要
    private final JwtService jwtService;
    private final LoginGuardService loginGuardService;
    private final SessionService sessionService;

    // 接收注册参数
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginGuardService loginGuardService,
            SessionService sessionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginGuardService = loginGuardService;
        this.sessionService = sessionService;
    }

    // 注册用户
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 规范邮箱
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 检查邮箱是否存在
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        // 生成密码哈希
        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        // 创建用户实体对象
        UserEntity user = new UserEntity(
                email,
                passwordHash
        );

        try {
            // 保存到数据库
            UserEntity savedUser = userRepository.saveAndFlush(user);

            // 返回注册结果
            return new RegisterResponse(
                    savedUser.getId(),
                    savedUser.getEmail()
            );

        } catch (DataIntegrityViolationException e) {
            // 处理数据库约束冲突
            throw new EmailAlreadyExistsException();
        }
    }

    /**
     * 登录入口
     */
    public LoginResponse login(
            LoginRequest request,
            String remoteIp
    ) {

        LoginGuardService.Result result =
                loginGuardService.authenticate(
                        request.email(),
                        request.password(),
                        remoteIp
                );

        if (result.status()
                == LoginGuardService.Status.RATE_LIMITED) {

            throw new LoginRateLimitException(
                    result.retryAfterSeconds()
            );
        }

        if (result.status()
                == LoginGuardService.Status.INVALID_CREDENTIALS) {

            throw new InvalidCredentialsException();
        }

        SessionService.SessionTokens tokens =
                sessionService.createSession(result.userId());

        String accessToken = jwtService.generateToken(
                result.userId(),
                tokens.sessionId()
        );

        return new LoginResponse(
                accessToken,
                tokens.refreshToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                sessionService.getRefreshExpirationSeconds()
        );
    }

    /**
     * 刷新Token。
     *
     * SessionService先在事务中完成轮换或重放撤销，
     * 然后才由本方法决定是否返回401。
     */
    public LoginResponse refresh(RefreshRequest request) {

        SessionService.RefreshResult result =
                sessionService.rotate(request.refreshToken());

        if (result.status()
                != SessionService.RefreshStatus.SUCCESS) {

            throw new InvalidRefreshTokenException();
        }

        String accessToken = jwtService.generateToken(
                result.userId(),
                result.sessionId()
        );

        return new LoginResponse(
                accessToken,
                result.refreshToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                sessionService.getRemainingSeconds(
                        result.sessionId()
                )
        );
    }

    /**
     * 退出当前设备。
     */
    public void logout(AuthenticatedUser currentUser) {

        sessionService.revokeSession(
                currentUser.sessionId(),
                currentUser.id()
        );
    }

    /**
     * 退出全部设备。
     *
     * 再次输入密码，并通过03-4的防护检查，
     * 避免这个敏感操作成为新的无限密码猜测入口。
     */
    public void logoutAll(
            AuthenticatedUser currentUser,
            LogoutAllRequest request,
            String remoteIp
    ) {

        LoginGuardService.Result result =
                loginGuardService.authenticate(
                        currentUser.email(),
                        request.password(),
                        remoteIp
                );

        if (result.status()
                == LoginGuardService.Status.RATE_LIMITED) {

            throw new LoginRateLimitException(
                    result.retryAfterSeconds()
            );
        }

        if (result.status()
                != LoginGuardService.Status.SUCCESS
                || !currentUser.id().equals(result.userId())) {

            throw new InvalidCredentialsException();
        }

        sessionService.revokeAllSessions(currentUser.id());
    }

}
