package com.ddd.webbb.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.ddd.webbb.ai.infrastructure.adapter.OpenAiEmotionAnalyzer;
import com.ddd.webbb.emotion.domain.EmotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

class OpenAiEmotionAnalyzerTest {

    private ChatClient chatClient;
    private OpenAiEmotionAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        analyzer = new OpenAiEmotionAnalyzer(chatClient, "게시글: {content}");
    }

    @Test
    void 정상_응답을_파싱하여_반환한다() {
        String jsonResponse = """
            {"emotionType":"LONELINESS","hp":30,"confidence":0.85,"reason":"극도의 외로움"}
            """;
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(jsonResponse);

        PostContent content = new PostContent(2L, "아무도 나를 이해하지 못해");
        EmotionAnalysisResult result = analyzer.analyze(content);

        assertThat(result.emotionType()).isEqualTo(EmotionType.LONELINESS);
        assertThat(result.hp()).isEqualTo(30);
    }

    @Test
    void providerName은_OPENAI이다() {
        assertThat(analyzer.providerName()).isEqualTo("OPENAI");
    }

    @Test
    void API_호출_실패는_RetryableAiException을_던진다() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("rate limit exceeded"));

        PostContent content = new PostContent(2L, "테스트 내용");

        assertThatThrownBy(() -> analyzer.analyze(content))
            .isInstanceOf(RetryableAiException.class);
    }
}
