
package com.healthpilot.server.auth.security;

import com.healthpilot.server.user.repository.UserRepository;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 身份认证过滤器
 * 接口是否允许匿名访问，
 * 最终由 SecurityConfig 中的权限规则决定。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 第一步：读取前端发送的 Authorization 请求头
        String authorizationHeader = request.getHeader("Authorization");

        // 没有 Bearer Token，继续交给后面的安全规则处理
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 第二步：去掉 "Bearer " 前缀，取得JWT字符串
        String token = authorizationHeader.substring(7);

        try {

            // 第三步：验证JWT并取得用户ID
            // JwtService会检查签名、签发者和过期时间
            Long userId = jwtService.getUserIdFromToken(token);

            // 第四步：检查该用户是否仍然存在于数据库
            userRepository.findById(userId).ifPresent(user -> {

                // 创建安全的当前用户对象，不包含密码
                AuthenticatedUser principal = new AuthenticatedUser(
                        user.getId(),
                        user.getEmail()
                );

                // 创建一个已经通过认证的Authentication对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of()
                        );

                // 保存当前HTTP请求的附加信息
                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // 创建新的安全上下文
                SecurityContext context =
                        SecurityContextHolder.createEmptyContext();

                context.setAuthentication(authentication);

                // 告诉Spring Security：当前请求的身份已认证
                SecurityContextHolder.setContext(context);
            });

        } catch (JwtException | IllegalArgumentException e) {

            // Token无效、过期、签名错误或用户ID格式错误
            // 清空认证信息，不把异常详情返回给前端
            SecurityContextHolder.clearContext();
        }

        // 第五步：让请求继续进入Spring Security后续过滤器
        // 如果是受保护接口且没有认证，最终返回401
        filterChain.doFilter(request, response);
    }
}
