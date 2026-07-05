package com.ddd.webbb.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.ai.domain.CommentSummaryResult;
import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
    }

    @Test
    void 정상_JSON을_파싱한다() {
        String json = "{\"summary\":\"요약 내용\",\"tone\":\"CALM\"}";

        CommentSummaryResult result =
                parser.parse(
                        json,
                        CommentSummaryResult.class,
                        () -> new CommentSummaryResult("요약 실패", "NEUTRAL"));

        assertThat(result.summary()).isEqualTo("요약 내용");
        assertThat(result.tone()).isEqualTo("CALM");
    }

    @Test
    void 마크다운_코드블록으로_감싼_JSON도_파싱한다() {
        String fenced =
                """
                ```json
                {"summary":"코드블록 요약","tone":"CALM"}
                ```
                """;

        CommentSummaryResult result =
                parser.parse(
                        fenced,
                        CommentSummaryResult.class,
                        () -> new CommentSummaryResult("요약 실패", "NEUTRAL"));

        assertThat(result.summary()).isEqualTo("코드블록 요약");
    }

    @Test
    void 언어_표기_없는_코드블록도_파싱한다() {
        String fenced =
                """
                ```
                {"emotionType":"ANXIETY","hp":30,"confidence":0.9,"reason":"불안"}
                ```
                """;

        EmotionAnalysisResult result =
                parser.parse(
                        fenced, EmotionAnalysisResult.class, EmotionAnalysisResult::safeDefault);

        assertThat(result.hp()).isEqualTo(30);
    }

    @Test
    void 파싱_실패시_fallback을_반환한다() {
        EmotionAnalysisResult result =
                parser.parse(
                        "JSON 아님", EmotionAnalysisResult.class, EmotionAnalysisResult::safeDefault);

        assertThat(result).isEqualTo(EmotionAnalysisResult.safeDefault());
    }

    @Test
    void 유효하지_않은_결과는_fallback을_반환한다() {
        String invalidHp =
                "{\"emotionType\":\"ANXIETY\",\"hp\":99,\"confidence\":0.9,\"reason\":\"범위 밖\"}";

        EmotionAnalysisResult result =
                parser.parse(
                        invalidHp, EmotionAnalysisResult.class, EmotionAnalysisResult::safeDefault);

        assertThat(result).isEqualTo(EmotionAnalysisResult.safeDefault());
    }
}
