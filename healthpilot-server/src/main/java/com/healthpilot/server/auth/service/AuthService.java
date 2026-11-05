
package com.healthpilot.server.auth.service;

// 响应和请求对象：接收传来的注册信息，返回用户ID和邮箱
import com.healthpilot.server.auth.dto.RegisterRequest;
import com.healthpilot.server.auth.dto.RegisterResponse;
// 自定义异常：当邮箱已经被注册时抛出
import com.healthpilot.server.auth.exception.EmailAlreadyExistsException;
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
    // 接收注册参数
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
