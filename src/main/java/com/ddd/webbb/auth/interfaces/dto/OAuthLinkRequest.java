package com.ddd.webbb.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthLinkRequest(
        @Schema(example = "provider-access-token") @NotBlank(message = "OAuth 액세스 토큰은 필수입니다.")
                String oauthAccessToken) {}
