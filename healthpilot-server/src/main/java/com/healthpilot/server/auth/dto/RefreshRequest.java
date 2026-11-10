
package com.healthpilot.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "Refresh Token不能为空")
        @Size(max = 200, message = "Refresh Token格式不合法")
        String refreshToken
) {
}
