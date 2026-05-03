package com.ddd.webbb.user.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @Schema(example = "test@test.com")
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "이메일 형식이 올바르지 않습니다.")
                String email,
        @Schema(example = "ogu")
                @NotBlank(message = "닉네임은 필수입니다.")
                @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
                String nickname) {}
