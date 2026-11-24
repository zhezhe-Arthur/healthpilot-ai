
package com.healthpilot.server.healthprofile.controller;

import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.common.ApiResponse;

import com.healthpilot.server.healthprofile.dto.CreateHealthProfileRequest;
import com.healthpilot.server.healthprofile.dto.UpdateHealthProfileRequest;
import com.healthpilot.server.healthprofile.dto.HealthProfileResponse;
import com.healthpilot.server.healthprofile.service.HealthProfileService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/health-profile")
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    public HealthProfileController(
            HealthProfileService healthProfileService
    ) {
        this.healthProfileService = healthProfileService;
    }

    /**
     * 创建当前用户的健康画像。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HealthProfileResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateHealthProfileRequest request
    ) {
        HealthProfileResponse response =
                healthProfileService.create(
                        currentUser.id(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 查询当前用户的健康画像。
     */
    @GetMapping
    public ApiResponse<HealthProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                healthProfileService.getMyProfile(currentUser.id())
        );
    }

    /**
     * 局部修改当前用户的健康画像。
     */
    @PatchMapping
    public ApiResponse<HealthProfileResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateHealthProfileRequest request
    ) {
        return ApiResponse.success(
                healthProfileService.update(
                        currentUser.id(),
                        request
                )
        );
    }
}
