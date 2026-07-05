package com.ddd.webbb.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.application.AiResponseParser;
import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentSummaryServiceTest {

    private AiGateway aiGateway;
    private CommentSummaryService service;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        service =
                new CommentSummaryService(
                        aiGateway, new AiResponseParser(new ObjectMapper()), "댓글: {content}");
    }

    @Test
    void 정상_응답을_파싱하여_반환한다() {
        String json = "{\"summary\":\"따뜻한 위로 댓글\",\"tone\":\"CALM\"}";
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(json, "OPENAI"));

        CommentSummaryResponse response = service.summarize(1L, "힘내세요, 잘 될 거예요.");

        assertThat(response.summary()).isEqualTo("따뜻한 위로 댓글");
        assertThat(response.tone()).isEqualTo("CALM");
        assertThat(response.usedProvider()).isEqualTo("OPENAI");
    }

    @Test
    void 파싱_실패시_기본값을_반환한다() {
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult("잘못된 JSON", "OPENAI"));

        CommentSummaryResponse response = service.summarize(2L, "댓글 내용");

        assertThat(response.summary()).isEqualTo("요약 실패");
        assertThat(response.tone()).isEqualTo("NEUTRAL");
        assertThat(response.usedProvider()).isEqualTo("OPENAI");
    }

    @Test
    void 유효하지_않은_응답도_기본값을_반환한다() {
        String invalidJson = "{\"summary\":\"\",\"tone\":\"CALM\"}";
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(invalidJson, "OPENAI"));

        CommentSummaryResponse response = service.summarize(3L, "댓글");

        assertThat(response.summary()).isEqualTo("요약 실패");
    }

    @Test
    void 게이트웨이_호출_실패시_STATIC_폴백을_반환한다() {
        given(aiGateway.call(anyString()))
                .willThrow(
                        new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "모든 프로바이더 실패"));

        CommentSummaryResponse response = service.summarize(4L, "댓글");

        assertThat(response.summary()).isEqualTo("요약 실패");
        assertThat(response.tone()).isEqualTo("NEUTRAL");
        assertThat(response.usedProvider()).isEqualTo("STATIC");
    }

    @Test
    void 프롬프트에_댓글_텍스트가_치환되어_전달된다() {
        String json = "{\"summary\":\"요약\",\"tone\":\"NEUTRAL\"}";
        given(aiGateway.call(contains("실제 댓글 내용"))).willReturn(new AiGatewayResult(json, "OPENAI"));

        service.summarize(5L, "실제 댓글 내용");

        verify(aiGateway).call(contains("실제 댓글 내용"));
    }
}
