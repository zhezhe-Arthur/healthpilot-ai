
package com.healthpilot.server.auth.controller;

import com.healthpilot.server.auth.dto.LoginRequest;
import com.healthpilot.server.auth.dto.LoginResponse;
import com.healthpilot.server.auth.dto.RegisterRequest;
import com.healthpilot.server.auth.dto.RegisterResponse;
import com.healthpilot.server.auth.service.AuthService;
import com.healthpilot.server.common.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
