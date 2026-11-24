
package com.healthpilot.server.healthplan.controller;

import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.common.ApiResponse;
import com.healthpilot.server.healthplan.dto.*;
import com.healthpilot.server.healthplan.service.HealthPlanService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/health-plans")
public class HealthPlanController {

    private final HealthPlanService healthPlanService;

    public HealthPlanController(
            HealthPlanService healthPlanService
    ) {
        this.healthPlanService = healthPlanService;
    }

    // 1. 创建或重新生成健康计划草稿
    @PostMapping("/draft")
    public ApiResponse<HealthPlanResponse> saveDraft(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SaveHealthPlanDraftRequest request
    ) {
        return ApiResponse.success(
                healthPlanService.saveDraft(
                        currentUser.id(),
                        request
                )
        );
    }

    // 2. 查询指定周的健康计划
    @GetMapping("/{weekStartDate}")
    public ApiResponse<HealthPlanResponse> getByWeek(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate
    ) {
        return ApiResponse.success(
                healthPlanService.getByWeek(
                        currentUser.id(),
                        weekStartDate
                )
        );
    }

    // 3. 查询当前用户的全部历史计划
    @GetMapping
    public ApiResponse<List<HealthPlanResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                healthPlanService.list(currentUser.id())
        );
    }

    // 4. 用户确认七天健康计划
    @PostMapping("/{planId}/confirm")
    public ApiResponse<HealthPlanResponse> confirm(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long planId,
            @Valid @RequestBody ConfirmHealthPlanRequest request
    ) {
        return ApiResponse.success(
                healthPlanService.confirm(
                        currentUser.id(),
                        planId,
                        request
                )
        );
    }
}
