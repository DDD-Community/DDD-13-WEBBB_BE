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
import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.infrastructure.observability.AiMetricsLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAnalysisServiceTest {

    private AiGateway aiGateway;
    private CrisisFilter crisisFilter;
    private AiMetricsLogger metricsLogger;
    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        crisisFilter = mock(CrisisFilter.class);
        metricsLogger = mock(AiMetricsLogger.class);
        given(metricsLogger.recordAndLog(any(), any()))
                .willAnswer(
                        inv -> {
                            Supplier<AiAnalysisResponse> supplier = inv.getArgument(1);
                            return supplier.get();
                        });
        service =
                new AiAnalysisService(
                        aiGateway,
                        crisisFilter,
                        metricsLogger,
                        new ObjectMapper(),
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
    void Static_폴백_결과도_정상_파싱된다() {
        PostContent content = new PostContent(1L, "힘들어요");
        String staticJson =
                "{\"emotionType\":\"LETHARGY\",\"hp\":10,\"confidence\":0.0,\"reason\":\"fallback\"}";
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(staticJson, "STATIC"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("LETHARGY");
        assertThat(response.usedProvider()).isEqualTo("STATIC");
    }
}
