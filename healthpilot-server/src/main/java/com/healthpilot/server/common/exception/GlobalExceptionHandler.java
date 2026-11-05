
package com.healthpilot.server.common.exception;

import com.healthpilot.server.auth.exception.EmailAlreadyExistsException;
import com.healthpilot.server.common.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    // 处理 JSON 请求格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(
            HttpMessageNotReadableException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "请求JSON格式不正确"));
    }
}
