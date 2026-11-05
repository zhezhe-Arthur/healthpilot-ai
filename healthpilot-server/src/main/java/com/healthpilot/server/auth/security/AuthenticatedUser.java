package com.healthpilot.server.auth.security;

// 当前已登录用户的信息
public record AuthenticatedUser(
        Long id,
        String email
) {
}
