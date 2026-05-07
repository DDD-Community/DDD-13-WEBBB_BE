package com.ddd.webbb.user.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Schema(example = "newogu") @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.") String nickname,
        @Schema(example = "PLANNING") String jobRole,
        @Schema(example = "YEAR_5") String careerYear) {}
