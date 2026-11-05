
package com.healthpilot.server.config;

import com.healthpilot.server.auth.security.JwtAuthenticationFilter;
import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置整个Spring Security HTTP安全规则
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository
    ) throws Exception {

        // JWT认证过滤器由我们手动创建并加入安全过滤器链
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository
                );

        return http

                // 当前使用Authorization请求头携带JWT，
                // 不依赖Cookie会话进行身份认证
                .csrf(AbstractHttpConfigurer::disable)

                // 不使用Spring默认的表单登录
                .formLogin(AbstractHttpConfigurer::disable)

                // 不使用HTTP Basic用户名密码认证
                .httpBasic(AbstractHttpConfigurer::disable)

                // 不使用默认的Session登出机制
                .logout(AbstractHttpConfigurer::disable)

                // 服务端不创建登录Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // 配置接口访问权限
                .authorizeHttpRequests(auth -> auth

                        // 注册、登录接口允许匿名访问
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()

                        // 服务健康检查接口允许匿名访问
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/health"
                        ).permitAll()

                        // 保留正常的框架错误响应处理
                        .requestMatchers("/error").permitAll()

                        // 其他接口必须登录
                        .anyRequest().authenticated()
                )

                // 统一处理未认证和权限不足的响应
                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        writeError(
                                                response,
                                                401,
                                                "未登录或登录已过期"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        writeError(
                                                response,
                                                403,
                                                "没有访问权限"
                                        )
                        )
                )

                // 在权限判断之前执行JWT身份认证
                .addFilterAfter(
                        jwtFilter,
                        LogoutFilter.class
                )

                .build();
    }

    /**
     * 将Spring Security错误转换成项目统一JSON结构
     *
     * 当前消息为代码内固定字符串，
     * 不拼接用户输入的数据。
     */
    private static void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"code\":" + status
                        + ",\"message\":\"" + message
                        + "\",\"data\":null}"
        );
    }
}
