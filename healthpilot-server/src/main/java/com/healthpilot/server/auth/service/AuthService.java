
package com.healthpilot.server.auth.service;

// 响应和请求对象：接收传来的注册信息，返回用户ID和邮箱
import com.healthpilot.server.auth.dto.RegisterRequest;
import com.healthpilot.server.auth.dto.RegisterResponse;

import com.healthpilot.server.auth.dto.LoginRequest;
import com.healthpilot.server.auth.dto.LoginResponse;
// 自定义异常：当邮箱已经被注册时抛出
import com.healthpilot.server.auth.exception.EmailAlreadyExistsException;

import com.healthpilot.server.auth.exception.InvalidCredentialsException;
import com.healthpilot.server.auth.security.JwtService;
// 数据库用户实体类
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

    // 接收注册参数
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    // 用户登录：只读取用户数据，不修改数据库
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 第一步：统一邮箱格式
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 第二步：根据邮箱查询数据库中的用户
        // findByEmail返回Optional，可能有用户，也可能没有
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        // 第三步：比较明文密码与数据库中的BCrypt哈希
        boolean passwordMatched = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        // 第四步：密码不匹配，登录失败
        if (!passwordMatched) {
            throw new InvalidCredentialsException();
        }

        // 第五步：验证成功，使用用户ID生成JWT
        String accessToken = jwtService.generateToken(user.getId());

        // 第六步：向Controller返回登录结果
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }
}
