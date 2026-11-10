
package com.healthpilot.server.auth.controller;

import com.healthpilot.server.auth.dto.*;
import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.auth.service.AuthService;
import com.healthpilot.server.common.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // 用户登录接口
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        // 使用Servlet提供的客户端IP
        // 暂不信任客户端自行填写的X-Forwarded-For
        String remoteIp = httpRequest.getRemoteAddr();

        LoginResponse response = authService.login(
                request,
                remoteIp
        );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    // 刷新：使用Refresh Token，不需要Access Token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        LoginResponse response = authService.refresh(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 退出当前会话，需要有效Access Token
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        authService.logout(currentUser);

        return ResponseEntity.ok(
                ApiResponse.success((Void) null)
        );
    }

    // 撤销全部会话，需要有效Access Token和当前密码
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody LogoutAllRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.logoutAll(
                currentUser,
                request,
                httpRequest.getRemoteAddr()
        );

        return ResponseEntity.ok(
                ApiResponse.success((Void) null)
        );
    }
}
