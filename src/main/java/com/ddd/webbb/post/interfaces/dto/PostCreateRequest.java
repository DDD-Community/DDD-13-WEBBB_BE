package com.ddd.webbb.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PostCreateRequest(
        @Schema(example = "면접에서 계속 떨어져서 점점 자신감이 사라져요.") @NotBlank(message = "게시글 내용은 필수입니다.")
                String content,
        @Schema(example = "COMFORT_ME") String commentTone) {}
