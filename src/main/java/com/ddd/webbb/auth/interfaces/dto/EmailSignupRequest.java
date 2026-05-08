package com.ddd.webbb.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailSignupRequest(
        @Schema(example = "test@test.com")
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "이메일 형식이 올바르지 않습니다.")
                String email,
        @Schema(example = "password123!")
                @NotBlank(message = "비밀번호는 필수입니다.")
                @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
                String password,
        @Schema(example = "ogu")
                @NotBlank(message = "닉네임은 필수입니다.")
                @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
                String nickname,
        @Schema(example = "DEVELOPMENT") String jobRole,
        @Schema(example = "NEWCOMER") String careerYear) {}
