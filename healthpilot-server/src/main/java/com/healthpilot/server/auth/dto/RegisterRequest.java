
package com.healthpilot.server.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record RegisterRequest(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 190, message = "邮箱长度不能超过190个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须在8到64个字符之间")
        String password

) {

    // 限制 BCrypt 密码字节长度
    @AssertTrue(message = "密码UTF-8长度不能超过72字节")
    public boolean isPasswordByteLengthValid() {
        return password == null
                || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
