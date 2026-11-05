
package com.healthpilot.server.user.controller;

import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.common.ApiResponse;
import com.healthpilot.server.user.dto.UserMeResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /**
     * 获取当前登录用户信息
     *
     * @AuthenticationPrincipal：
     * 从SecurityContext读取经过认证的用户。
     *
     * 不接收前端自行传入的用户ID，
     * 避免用户通过修改参数读取其他人的资料。
     */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        UserMeResponse response = new UserMeResponse(
                currentUser.id(),
                currentUser.email()
        );

        return ApiResponse.success(response);
    }
}
