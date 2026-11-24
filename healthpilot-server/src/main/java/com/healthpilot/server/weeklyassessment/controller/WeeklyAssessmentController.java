
package com.healthpilot.server.weeklyassessment.controller;

import com.healthpilot.server.auth.security.AuthenticatedUser;
import com.healthpilot.server.common.ApiResponse;
import com.healthpilot.server.weeklyassessment.dto.CreateWeeklyAssessmentRequest;
import com.healthpilot.server.weeklyassessment.dto.UpdateWeeklyAssessmentRequest;
import com.healthpilot.server.weeklyassessment.dto.WeeklyAssessmentResponse;
import com.healthpilot.server.weeklyassessment.service.WeeklyAssessmentService;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/health-weekly-assessments")
public class WeeklyAssessmentController {

    private final WeeklyAssessmentService assessmentService;

    public WeeklyAssessmentController(
            WeeklyAssessmentService assessmentService
    ) {
        this.assessmentService = assessmentService;
    }

    // 创建每周健康评估
    @PostMapping
    public ResponseEntity<ApiResponse<WeeklyAssessmentResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateWeeklyAssessmentRequest request
    ) {
        WeeklyAssessmentResponse result =
                assessmentService.create(currentUser.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }

    // 查询指定周的健康评估
    @GetMapping("/{weekStartDate}")
    public ApiResponse<WeeklyAssessmentResponse> get(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate
    ) {
        return ApiResponse.success(
                assessmentService.get(currentUser.id(), weekStartDate)
        );
    }

    // 查询指定时间范围内的历史评估
    @GetMapping
    public ApiResponse<List<WeeklyAssessmentResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ApiResponse.success(
                assessmentService.list(
                        currentUser.id(),
                        startDate,
                        endDate
                )
        );
    }

    // 修改每周健康评估
    @PatchMapping("/{weekStartDate}")
    public ApiResponse<WeeklyAssessmentResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,

            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate,

            @Valid @RequestBody UpdateWeeklyAssessmentRequest request
    ) {
        return ApiResponse.success(
                assessmentService.update(
                        currentUser.id(),
                        weekStartDate,
                        request
                )
        );
    }
}
