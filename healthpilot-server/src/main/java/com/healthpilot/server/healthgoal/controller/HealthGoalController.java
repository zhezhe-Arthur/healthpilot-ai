
package com.healthpilot.server.healthgoal.controller;

import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.common.ApiResponse;

import com.healthpilot.server.healthgoal.dto.CreateHealthGoalRequest;
import com.healthpilot.server.healthgoal.dto.HealthGoalResponse;
import com.healthpilot.server.healthgoal.dto.UpdateHealthGoalRequest;
import com.healthpilot.server.healthgoal.entity.HealthGoalEntity.GoalStatus;
import com.healthpilot.server.healthgoal.service.HealthGoalService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health-goals")
public class HealthGoalController {

    private final HealthGoalService healthGoalService;

    public HealthGoalController(
            HealthGoalService healthGoalService
    ) {
        this.healthGoalService = healthGoalService;
    }

    // 创建健康目标
    @PostMapping
    public ResponseEntity<ApiResponse<HealthGoalResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateHealthGoalRequest request
    ) {
        HealthGoalResponse response = healthGoalService.create(
                currentUser.id(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // 查询目标列表，可以按status过滤
    @GetMapping
    public ApiResponse<List<HealthGoalResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(
                    name = "status",
                    required = false
            ) GoalStatus status
    ) {
        return ApiResponse.success(
                healthGoalService.getMyGoals(
                        currentUser.id(),
                        status
                )
        );
    }

    // 查询指定目标
    @GetMapping("/{id}")
    public ApiResponse<HealthGoalResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("id") Long id
    ) {
        return ApiResponse.success(
                healthGoalService.getMyGoal(
                        currentUser.id(),
                        id
                )
        );
    }

    // 更新目标及其状态
    @PatchMapping("/{id}")
    public ApiResponse<HealthGoalResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateHealthGoalRequest request
    ) {
        return ApiResponse.success(
                healthGoalService.update(
                        currentUser.id(),
                        id,
                        request
                )
        );
    }
}
