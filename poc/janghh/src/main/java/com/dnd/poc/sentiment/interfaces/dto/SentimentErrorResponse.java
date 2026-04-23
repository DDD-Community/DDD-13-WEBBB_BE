package com.dnd.poc.sentiment.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "감성 분석 실패 응답")
public record SentimentErrorResponse(
        @Schema(description = "요청 성공 여부", example = "false") boolean success,
        @Schema(description = "오류 메시지", example = "유효하지 않은 요청입니다.") String message,
        @Schema(description = "필드 검증 오류 목록") List<SentimentFieldErrorResponse> errors,
        @Schema(description = "응답 시각", example = "2026-04-22T10:00:00") LocalDateTime timestamp) {

    @Schema(description = "필드 단위 오류 정보")
    public record SentimentFieldErrorResponse(
            @Schema(description = "오류가 발생한 필드명", example = "text") String field,
            @Schema(description = "오류 사유", example = "분석할 텍스트를 입력해주세요.") String reason) {}
}
