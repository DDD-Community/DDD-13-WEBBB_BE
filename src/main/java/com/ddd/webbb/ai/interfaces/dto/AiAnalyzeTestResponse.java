package com.ddd.webbb.ai.interfaces.dto;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 감정 분석 테스트 응답")
public record AiAnalyzeTestResponse(
    @Schema(
        description = "분석된 대표 감정 유형",
        allowableValues = {"ANXIETY", "LETHARGY", "LONELINESS", "SELF_DEPRECATION", "IRRITATION"},
        example = "ANXIETY"
    )
    String emotionType,
    @Schema(description = "감정 강도에 따라 계산된 몬스터 HP", allowableValues = {"10", "20", "30"}, example = "20")
    int hp,
    @Schema(description = "AI 분류 신뢰도", example = "0.91")
    double confidence,
    @Schema(description = "감정 분류 근거", example = "불안과 긴장 표현이 반복적으로 드러남")
    String reason,
    @Schema(description = "위기 키워드 사전 감지 여부", example = "false")
    boolean crisisDetected,
    @Schema(description = "최종 응답을 생성한 제공자", allowableValues = {"CRISIS_FILTER", "CLAUDE", "OPENAI", "STATIC"}, example = "CLAUDE")
    String usedProvider
) {
    public static AiAnalyzeTestResponse from(AiAnalysisResponse response) {
        return new AiAnalyzeTestResponse(
            response.emotionType(),
            response.hp(),
            response.confidence(),
            response.reason(),
            response.crisisDetected(),
            response.usedProvider()
        );
    }
}
