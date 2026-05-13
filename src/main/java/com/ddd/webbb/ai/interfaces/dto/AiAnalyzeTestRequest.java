package com.ddd.webbb.ai.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 감정 분석 테스트 요청")
public record AiAnalyzeTestRequest(
    @NotBlank
    @Schema(
        description = "감정 분석할 게시글 본문",
        example = "면접 결과를 기다리는데 계속 불안하고 심장이 떨려요."
    )
    String content
) {
}
