package com.dnd.poc.sentiment.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "감성 분석 성공 응답")
public record SentimentAnalyzeSuccessResponse(
        @Schema(description = "요청 성공 여부", example = "true") boolean success,
        @Schema(description = "응답 메시지", example = "감성 분석이 완료되었습니다.") String message,
        @Schema(description = "감성 분석 결과") SentimentResponse data,
        @Schema(description = "응답 시각", example = "2026-04-22T10:00:00") LocalDateTime timestamp) {}
