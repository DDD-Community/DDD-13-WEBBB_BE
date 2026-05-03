package com.dnd.poc.retrospective.interfaces.dto;

import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "KPT 분석 결과")
public record RetrospectiveResponse(
        @Schema(description = "잘하고 있는 점", example = "[\"끝까지 시도한 점\",\"본인 약점을 객관화한 점\"]")
        List<String> keep,

        @Schema(description = "객관적·개선 가능한 사실", example = "[\"시간 분배 전략 부재\"]")
        List<String> problem,

        @Schema(description = "내일 실행할 수 있는 작은 액션", example = "[\"자료구조 30분 복습\"]")
        @JsonProperty("try") List<String> tries,

        @Schema(description = "따뜻한 위로·응원 한마디", example = "오늘도 시도한 것만으로 충분해요.")
        @JsonProperty("cheer_message") String cheerMessage
) {
    public static RetrospectiveResponse from(KptAnalysis analysis) {
        return new RetrospectiveResponse(
                analysis.keep(),
                analysis.problem(),
                analysis.tries(),
                analysis.cheerMessage()
        );
    }
}
