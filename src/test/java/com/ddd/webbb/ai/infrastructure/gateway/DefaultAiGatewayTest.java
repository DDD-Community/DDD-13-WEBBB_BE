package com.ddd.webbb.ai.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.PermanentAiException;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultAiGatewayTest {

    private AiProvider openAi;
    private AiProvider staticProvider;
    private DefaultAiGateway gateway;

    @BeforeEach
    void setUp() {
        openAi = mock(AiProvider.class);
        staticProvider = mock(AiProvider.class);
        given(openAi.providerName()).willReturn("OPENAI");
        given(staticProvider.providerName()).willReturn("STATIC");
        gateway = new DefaultAiGateway(List.of(openAi, staticProvider));
    }

    @Test
    void 첫번째_프로바이더_성공시_바로_반환한다() {
        given(openAi.call("prompt")).willReturn("openai-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("openai-response");
        assertThat(result.providerName()).isEqualTo("OPENAI");
        verify(staticProvider, never()).call(any());
    }

    @Test
    void RetryableAiException_발생시_Static으로_폴백한다() {
        given(openAi.call("prompt"))
                .willThrow(new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "timeout"));
        given(staticProvider.call("prompt")).willReturn("static-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("static-response");
        assertThat(result.providerName()).isEqualTo("STATIC");
    }

    @Test
    void PermanentAiException_발생시_Static으로_폴백한다() {
        given(openAi.call("prompt"))
                .willThrow(new PermanentAiException(AiErrorCode.INVALID_RESPONSE, "bad response"));
        given(staticProvider.call("prompt")).willReturn("static-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("static-response");
        assertThat(result.providerName()).isEqualTo("STATIC");
    }

    @Test
    void 모든_프로바이더_실패시_IllegalStateException을_던진다() {
        given(openAi.call("prompt")).willThrow(new RuntimeException("error"));
        given(staticProvider.call("prompt")).willThrow(new RuntimeException("error"));

        assertThatThrownBy(() -> gateway.call("prompt")).isInstanceOf(IllegalStateException.class);
    }
}
