package com.ddd.webbb.auth.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,
        @NotBlank(message = "인증 코드는 필수입니다.") @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
                String code,
        @NotBlank(message = "새 비밀번호는 필수입니다.")
                @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
                String newPassword) {}
