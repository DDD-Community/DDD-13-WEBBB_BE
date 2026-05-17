package com.ddd.webbb.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.ddd.webbb.ai.infrastructure.gateway.ClaudeAiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

class ClaudeAiProviderTest {

    private ChatClient chatClient;
    private ClaudeAiProvider provider;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        provider = new ClaudeAiProvider(chatClient);
    }

    @Test
    void 정상_호출시_원시_문자열을_반환한다() {
        String rawResponse =
                "{\"emotionType\":\"ANXIETY\",\"hp\":30,\"confidence\":0.9,\"reason\":\"불안\"}";
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(rawResponse);

        String result = provider.call("테스트 프롬프트");

        assertThat(result).isEqualTo(rawResponse);
    }

    @Test
    void providerName은_CLAUDE이다() {
        assertThat(provider.providerName()).isEqualTo("CLAUDE");
    }

    @Test
    void API_호출_실패는_RetryableAiException을_던진다() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("connection timeout"));

        assertThatThrownBy(() -> provider.call("테스트 프롬프트"))
                .isInstanceOf(RetryableAiException.class);
    }
}
