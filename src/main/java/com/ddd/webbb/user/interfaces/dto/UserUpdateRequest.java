package com.ddd.webbb.user.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Schema(example = "newname") @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
                String nickname,
        @Schema(example = "BACKEND") String jobType,
        @Schema(example = "3년차") String careerLevel) {}
