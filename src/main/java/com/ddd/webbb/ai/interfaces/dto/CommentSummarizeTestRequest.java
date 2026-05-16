package com.ddd.webbb.ai.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 AI 요약 테스트 요청")
public record CommentSummarizeTestRequest(
        @NotBlank
                @Schema(
                        description = "요약할 댓글 내용",
                        example = "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.")
                String content) {}
