package com.ddd.webbb.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentSummaryServiceTest {

    private AiGateway aiGateway;
    private CommentSummaryService service;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        service = new CommentSummaryService(aiGateway, new ObjectMapper(), "댓글: {content}");
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
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(invalidJson, "STATIC"));

        CommentSummaryResponse response = service.summarize(3L, "댓글");

        assertThat(response.summary()).isEqualTo("요약 실패");
    }

    @Test
    void 프롬프트에_댓글_텍스트가_치환되어_전달된다() {
        String json = "{\"summary\":\"요약\",\"tone\":\"NEUTRAL\"}";
        given(aiGateway.call(contains("실제 댓글 내용"))).willReturn(new AiGatewayResult(json, "OPENAI"));

        service.summarize(4L, "실제 댓글 내용");

        verify(aiGateway).call(contains("실제 댓글 내용"));
    }

    @Test
    void Static_폴백_결과도_정상_파싱된다() {
        String staticJson = "{\"summary\":\"기본 요약\",\"tone\":\"NEUTRAL\"}";
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(staticJson, "STATIC"));

        CommentSummaryResponse response = service.summarize(5L, "댓글");

        assertThat(response.usedProvider()).isEqualTo("STATIC");
        assertThat(response.summary()).isEqualTo("기본 요약");
    }
}
