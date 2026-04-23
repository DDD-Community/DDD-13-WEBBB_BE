package com.dnd.poc.sentiment.interfaces.dto;

import com.dnd.poc.sentiment.domain.SentimentLabel;
import com.dnd.poc.sentiment.domain.SentimentResult;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SentimentResponse(
        @Schema(description = "감성 분류 결과", example = "POSITIVE") SentimentLabel label,
        @Schema(description = "긍정/부정 방향성 점수", example = "0.87") double score,
        @Schema(description = "모델의 판단 신뢰도", example = "0.93") double confidence,
        @ArraySchema(schema = @Schema(description = "감성 판단에 사용된 핵심 키워드", example = "기분"))
                List<String> keywords,
        @Schema(description = "분석 근거 요약", example = "긍정 감성 표현이 명확하게 포함됨") String reason) {

    public static SentimentResponse from(SentimentResult result) {
        return new SentimentResponse(
                result.label(),
                result.score(),
                result.confidence(),
                result.keywords(),
                result.reason());
    }
}
