package com.dnd.webbb.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dnd.webbb.ai.application.dto.AnalysisResponse;
import com.dnd.webbb.ai.application.dto.EncouragementResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class AiEncouragementServiceTest {

    private ChatClient chatClient;
    private AiEncouragementService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        service = new AiEncouragementService(chatClient);
    }

    @Test
    void 위기_키워드_감지시_AI_호출_없이_안전_안내를_반환한다() {
        AnalysisResponse result = service.analyze("취업이 안돼서 죽고 싶어", "친구같은");

        assertThat(result.crisisDetected()).isTrue();
        assertThat(result.encouragementMessage()).contains("1393");
        assertThat(result.encouragementMessage()).contains("109");
        verify(chatClient, never()).prompt();
    }

    @Test
    void 자해_키워드도_위기로_감지한다() {
        AnalysisResponse result = service.analyze("자해하고 싶은 충동이 있어", "친구같은");

        assertThat(result.crisisDetected()).isTrue();
        assertThat(result.encouragementMessage()).contains("1393");
    }

    @Test
    void AI_호출_실패시_fallback_응답을_반환한다() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("API 연결 실패"));

        AnalysisResponse result = service.analyze("면접에서 떨어졌어", "친구같은");

        assertThat(result.crisisDetected()).isFalse();
        assertThat(result.encouragementMessage()).contains("잠시 후 다시 시도해주세요");
        assertThat(result.emotionTags()).isEmpty();
    }

    @Test
    void 정상_요청시_AI_응답을_반환한다() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(EncouragementResult.class))
                .thenReturn(
                        new EncouragementResult(
                                List.of("불안", "자존감저하"),
                                "면접 탈락으로 힘들어하고 있다",
                                "충분히 잘하고 있어",
                                "오늘 하루 산책하기"));

        AnalysisResponse result = service.analyze("면접에서 떨어졌어", "친구같은");

        assertThat(result.crisisDetected()).isFalse();
        assertThat(result.emotionTags()).containsExactly("불안", "자존감저하");
        assertThat(result.encouragementMessage()).isEqualTo("충분히 잘하고 있어");
    }
}
