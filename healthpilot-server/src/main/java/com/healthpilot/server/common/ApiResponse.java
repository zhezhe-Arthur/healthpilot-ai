
package com.healthpilot.server.common;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    // 统一成功响应
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                0,
                "success",
                data
        );
    }

    // 统一失败响应
    public static <T> ApiResponse<T> error(
            int code,
            String message
    ) {
        return new ApiResponse<>(
                code,
                message,
                null
        );
    }
}
