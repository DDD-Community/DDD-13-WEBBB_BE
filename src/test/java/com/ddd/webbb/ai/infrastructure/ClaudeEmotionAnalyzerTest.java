package com.ddd.webbb.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.domain.exception.PermanentAiException;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.ddd.webbb.ai.infrastructure.adapter.ClaudeEmotionAnalyzer;
import com.ddd.webbb.emotion.domain.EmotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

class ClaudeEmotionAnalyzerTest {

    private ChatClient chatClient;
    private ClaudeEmotionAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        analyzer = new ClaudeEmotionAnalyzer(chatClient, "게시글: {content}");
    }

    @Test
    void 정상_응답을_파싱하여_반환한다() {
        String jsonResponse =
                """
            {"emotionType":"ANXIETY","hp":30,"confidence":0.9,"reason":"불안 표현이 강함"}
            """;
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(jsonResponse);

        PostContent content = new PostContent(1L, "면접 때문에 너무 불안해요");
        EmotionAnalysisResult result = analyzer.analyze(content);

        assertThat(result.emotionType()).isEqualTo(EmotionType.ANXIETY);
        assertThat(result.hp()).isEqualTo(30);
        assertThat(result.confidence()).isEqualTo(0.9);
    }

    @Test
    void providerName은_CLAUDE이다() {
        assertThat(analyzer.providerName()).isEqualTo("CLAUDE");
    }

    @Test
    void 잘못된_JSON_응답은_PermanentAiException을_던진다() {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("잘못된 응답입니다");

        PostContent content = new PostContent(1L, "테스트 내용");

        assertThatThrownBy(() -> analyzer.analyze(content))
                .isInstanceOf(PermanentAiException.class);
    }

    @Test
    void API_호출_실패는_RetryableAiException을_던진다() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("connection timeout"));

        PostContent content = new PostContent(1L, "테스트 내용");

        assertThatThrownBy(() -> analyzer.analyze(content))
                .isInstanceOf(RetryableAiException.class);
    }
}
