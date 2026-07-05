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

    private AiProvider primary;
    private AiProvider secondary;
    private DefaultAiGateway gateway;

    @BeforeEach
    void setUp() {
        primary = mock(AiProvider.class);
        secondary = mock(AiProvider.class);
        given(primary.providerName()).willReturn("OPENAI");
        given(secondary.providerName()).willReturn("SECONDARY");
        gateway = new DefaultAiGateway(List.of(primary, secondary));
    }

    @Test
    void 첫번째_프로바이더_성공시_바로_반환한다() {
        given(primary.call("prompt")).willReturn("openai-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("openai-response");
        assertThat(result.providerName()).isEqualTo("OPENAI");
        verify(secondary, never()).call(any());
    }

    @Test
    void RetryableAiException_발생시_다음_프로바이더로_폴백한다() {
        given(primary.call("prompt"))
                .willThrow(new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "timeout"));
        given(secondary.call("prompt")).willReturn("secondary-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("secondary-response");
        assertThat(result.providerName()).isEqualTo("SECONDARY");
    }

    @Test
    void PermanentAiException_발생시_다음_프로바이더로_폴백한다() {
        given(primary.call("prompt"))
                .willThrow(new PermanentAiException(AiErrorCode.INVALID_RESPONSE, "bad response"));
        given(secondary.call("prompt")).willReturn("secondary-response");

        AiGatewayResult result = gateway.call("prompt");

        assertThat(result.rawResponse()).isEqualTo("secondary-response");
        assertThat(result.providerName()).isEqualTo("SECONDARY");
    }

    @Test
    void 모든_프로바이더_실패시_RetryableAiException을_던진다() {
        given(primary.call("prompt")).willThrow(new RuntimeException("error"));
        given(secondary.call("prompt")).willThrow(new RuntimeException("error"));

        assertThatThrownBy(() -> gateway.call("prompt")).isInstanceOf(RetryableAiException.class);
    }
}
