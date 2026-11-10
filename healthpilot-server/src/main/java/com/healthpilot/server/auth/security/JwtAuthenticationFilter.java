
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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            SessionService sessionService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // 验证JWT，并读取用户ID与会话ID
            JwtService.AccessIdentity identity =
                    jwtService.parseAccessToken(token);

            // 检查数据库中的会话是否已被撤销或过期
            boolean active = sessionService.isSessionActive(
                    identity.sessionId(),
                    identity.userId()
            );

            if (active) {
                userRepository.findById(identity.userId())
                        .ifPresent(user -> {

                            AuthenticatedUser principal =
                                    new AuthenticatedUser(
                                            user.getId(),
                                            user.getEmail(),
                                            identity.sessionId()
                                    );

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            List.of()
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContext context =
                                    SecurityContextHolder
                                            .createEmptyContext();

                            context.setAuthentication(authentication);

                            SecurityContextHolder.setContext(context);
                        });
            }

        } catch (JwtException | IllegalArgumentException e) {

            // JWT签名错误、过期或声明不合法
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
