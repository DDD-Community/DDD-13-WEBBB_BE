package com.dnd.webbb.user.interfaces.dto;

import com.dnd.webbb.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Schema(example = "newname") @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
                String nickname,
        @Schema(example = "INACTIVE") UserStatus status) {}
