
package com.healthpilot.server.config;

import com.healthpilot.server.auth.security.JwtAuthenticationFilter;
import com.healthpilot.server.auth.security.JwtService;
import com.healthpilot.server.auth.security.SessionService;
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

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            SessionService sessionService,
            UserRepository userRepository
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        sessionService,
                        userRepository
                );

        return http
                // 当前使用请求头+JSON传递凭证
                .csrf(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // 无需Access Token的认证入口
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/health"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // /logout、/logout-all及其他业务接口均要求认证
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, e) ->
                                        writeError(
                                                response,
                                                401,
                                                "未登录或登录已过期"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, e) ->
                                        writeError(
                                                response,
                                                403,
                                                "没有访问权限"
                                        )
                        )
                )

                .addFilterAfter(
                        jwtFilter,
                        LogoutFilter.class
                )

                .build();
    }

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
