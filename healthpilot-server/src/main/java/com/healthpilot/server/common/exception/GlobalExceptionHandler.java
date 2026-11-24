
package com.healthpilot.server.common.exception;

import com.healthpilot.server.auth.exception.EmailAlreadyExistsException;
import com.healthpilot.server.auth.exception.InvalidCredentialsException;
import com.healthpilot.server.auth.exception.LoginRateLimitException;
import com.healthpilot.server.common.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.healthpilot.server.auth.exception.InvalidRefreshTokenException;

import com.healthpilot.server.healthprofile.exception.HealthProfileBusinessException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.healthpilot.server.healthgoal.exception.HealthGoalBusinessException;

import com.healthpilot.server.weeklyassessment.exception.WeeklyAssessmentBusinessException;

import com.healthpilot.server.healthplan.exception.HealthPlanBusinessException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("请求参数不合法");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, message));
    }

    // 处理邮箱重复注册
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailExists(
            EmailAlreadyExistsException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, e.getMessage()));
    }

    // 登录失败
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
            InvalidCredentialsException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, e.getMessage()));
    }

    // 登录触发限流
    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(
            LoginRateLimitException e
    ) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        "Retry-After",
                        String.valueOf(e.getRetryAfterSeconds())
                )
                .body(ApiResponse.error(429, e.getMessage()));
    }

    // 处理 JSON 请求格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(
            HttpMessageNotReadableException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "请求JSON格式不正确"));
    }

    // Refresh Token无效、过期、撤销或重放。
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(
            InvalidRefreshTokenException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, e.getMessage()));
    }



    // 健康画像业务异常： 400 参数错误、404 未找到、409 重复创建或版本冲突。
    @ExceptionHandler(HealthProfileBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleHealthProfile(
            HealthProfileBusinessException e
    ) {
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS, VERSION_CONFLICT ->
                    HttpStatus.CONFLICT;
        };

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.error(
                                status.value(),
                                e.getMessage()
                        )
                );
    }

    //  数据库层检测到真正并发更新冲突。
    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            Exception e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ApiResponse.error(
                                409,
                                "数据已发生变化，请刷新后重试"
                        )
                );
    }

    /**
     * 健康目标模块的业务异常。
     * 1. NOT_FOUND：目标不存在或不属于当前用户，返回404
     * 2. INVALID_INPUT：目标参数不合法，返回400
     * 3. VERSION_CONFLICT：提交了旧版本数据，返回409
     * 4. INVALID_STATUS：目标状态不允许修改，返回409
     */
    @ExceptionHandler(HealthGoalBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleHealthGoal(
            HealthGoalBusinessException e
    ) {
        // 根据业务异常类型选择HTTP状态码
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case VERSION_CONFLICT, INVALID_STATUS ->
                    HttpStatus.CONFLICT;
        };

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.error(
                                status.value(),
                                e.getMessage()
                        )
                );
    }


    // 每周健康评估业务异常
    @ExceptionHandler(WeeklyAssessmentBusinessException.class)
    public ResponseEntity<?> handleWeeklyAssessmentException(
            WeeklyAssessmentBusinessException e
    ) {
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS, VERSION_CONFLICT -> HttpStatus.CONFLICT;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), e.getMessage()));
    }

    // 计划查询错误
    @ExceptionHandler(HealthPlanBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleHealthPlanBusiness(
            HealthPlanBusinessException e
    ) {
        HttpStatus status = switch (e.getReason()) {

            case NOT_FOUND -> HttpStatus.NOT_FOUND;

            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;

            case VERSION_CONFLICT, PLAN_CONFIRMED ->
                    HttpStatus.CONFLICT;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.error(
                        status.value(),
                        e.getMessage()
                ));
    }


}
