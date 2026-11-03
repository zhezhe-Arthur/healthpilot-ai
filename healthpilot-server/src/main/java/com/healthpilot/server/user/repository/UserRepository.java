
package com.healthpilot.server.user.repository;

import com.healthpilot.server.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, Long> {

    // 判断邮箱是否已注册
    boolean existsByEmail(String email);

    // 根据邮箱查询用户
    Optional<UserEntity> findByEmail(String email);
}
