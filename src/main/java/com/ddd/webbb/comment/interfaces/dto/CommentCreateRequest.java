package com.ddd.webbb.comment.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @Schema(description = "대댓글인 경우 부모 댓글 ID", example = "1") Long parentCommentId,
        @Schema(example = "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.")
                @NotBlank(message = "댓글 내용은 필수입니다.")
                String content) {}
