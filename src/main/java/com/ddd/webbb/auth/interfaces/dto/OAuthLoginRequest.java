package com.ddd.webbb.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
        @Schema(example = "provider-access-token") @NotBlank(message = "OAuth 액세스 토큰은 필수입니다.")
                String oauthAccessToken,
        @Schema(example = "ogu") @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.") String nickname,
        @Schema(example = "DEVELOPMENT") String jobRole,
        @Schema(example = "YEAR_3") String careerYear) {}
