package com.ddd.webbb.user.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Schema(example = "newogu") @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하여야 합니다.")
                String nickname,
        @Schema(example = "PLANNING") String jobType,
        @Schema(example = "3년차") String careerLevel) {}
