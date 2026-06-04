package com.ddd.webbb.comment.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequest(
        @Schema(example = "수정된 댓글 내용입니다.") @NotBlank(message = "댓글 내용은 필수입니다.") String content) {}
