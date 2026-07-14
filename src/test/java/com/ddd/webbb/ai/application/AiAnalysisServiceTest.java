package com.ddd.webbb.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.AiMetricsRecorder;
import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAnalysisServiceTest {

    private AiGateway aiGateway;
    private CrisisFilter crisisFilter;
    private AiMetricsRecorder metricsRecorder;
    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        crisisFilter = mock(CrisisFilter.class);
        metricsRecorder = mock(AiMetricsRecorder.class);
        given(metricsRecorder.recordAndLog(any(), any(), any()))
                .willAnswer(
                        inv -> {
                            Supplier<AiAnalysisResponse> supplier = inv.getArgument(1);
                            return supplier.get();
                        });
        service =
                new AiAnalysisService(
                        aiGateway,
                        crisisFilter,
                        metricsRecorder,
                        new AiResponseParser(new ObjectMapper()),
                        "게시글: {content}");
    }

    @Test
    void 정상_분석은_AiGateway를_통해_결과를_반환한다() {
        PostContent content = new PostContent(1L, "면접 때문에 너무 불안해요");
        String validJson =
                "{\"emotionType\":\"ANXIETY\",\"hp\":30,\"confidence\":0.9,\"reason\":\"불안 표현\"}";
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(validJson, "OPENAI"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("ANXIETY");
        assertThat(response.hp()).isEqualTo(30);
        assertThat(response.usedProvider()).isEqualTo("OPENAI");
        assertThat(response.crisisDetected()).isFalse();
    }

    @Test
    void 욕설_단어_목록을_응답에_전달한다() {
        PostContent content = new PostContent(1L, "진짜 ㅅㅂ하네 시1발");
        String json =
                "{\"emotionType\":\"IRRITATION\",\"hp\":20,\"confidence\":0.9,"
                        + "\"reason\":\"짜증\",\"profanityWords\":[\"ㅅㅂ\",\"시1발\"]}";
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(json, "OPENAI"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.profanityWords()).containsExactly("ㅅㅂ", "시1발");
    }

    @Test
    void 욕설_필드가_없는_v1_응답은_빈_목록으로_처리한다() {
        PostContent content = new PostContent(1L, "불안해요");
        String json =
                "{\"emotionType\":\"ANXIETY\",\"hp\":30,\"confidence\":0.9,\"reason\":\"불안\"}";
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(json, "OPENAI"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.profanityWords()).isEmpty();
    }

    @Test
    void 위기_감지시_AiGateway를_호출하지_않는다() {
        PostContent content = new PostContent(1L, "죽고 싶어요");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.crisis("죽고 싶"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.usedProvider()).isEqualTo("CRISIS_FILTER");
        verify(aiGateway, never()).call(any());
    }

    @Test
    void 파싱_실패시_safeDefault를_사용한다() {
        PostContent content = new PostContent(1L, "힘들어요");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult("잘못된 JSON", "OPENAI"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("LETHARGY");
        assertThat(response.hp()).isEqualTo(10);
        assertThat(response.usedProvider()).isEqualTo("OPENAI");
    }

    @Test
    void 유효하지_않은_응답도_예외_대신_safeDefault를_사용한다() {
        PostContent content = new PostContent(1L, "힘들어요");
        String invalidJson =
                "{\"emotionType\":\"ANXIETY\",\"hp\":99,\"confidence\":0.9,\"reason\":\"범위 밖 hp\"}";
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(invalidJson, "OPENAI"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("LETHARGY");
        assertThat(response.hp()).isEqualTo(10);
    }

    @Test
    void 게이트웨이_호출_실패시_STATIC_폴백을_반환한다() {
        PostContent content = new PostContent(1L, "힘들어요");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString()))
                .willThrow(
                        new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "모든 프로바이더 실패"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("LETHARGY");
        assertThat(response.hp()).isEqualTo(10);
        assertThat(response.usedProvider()).isEqualTo("STATIC");
    }
}
