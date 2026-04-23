package com.dnd.poc.sentiment.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SentimentRequest(
        @Schema(description = "감성 분석할 한국어 텍스트", example = "오늘 발표가 잘 끝나서 정말 기분이 좋아요.")
                @NotBlank(message = "분석할 텍스트를 입력해주세요.")
                @Size(max = 2000, message = "텍스트는 2000자 이하여야 합니다.")
                String text) {}
