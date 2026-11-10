
package com.healthpilot.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutAllRequest(
        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不能超过64个字符")
        String password
) {
}
