package com.ddd.webbb.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(example = "refresh-token") @NotBlank(message = "리프레시 토큰은 필수입니다.")
                String refreshToken) {}
