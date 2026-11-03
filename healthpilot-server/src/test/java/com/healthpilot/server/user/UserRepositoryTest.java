
package com.healthpilot.server.user;

import com.healthpilot.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // 验证数据库连接与用户表查询
    @Test
    void shouldQueryUsersTable() {

        long count = userRepository.count();

        assertTrue(count >= 0);
    }
}
