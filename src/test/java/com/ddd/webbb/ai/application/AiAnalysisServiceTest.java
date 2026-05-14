package com.ddd.webbb.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.EmotionAnalyzer;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.emotion.domain.EmotionType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAnalysisServiceTest {

    private EmotionAnalyzer primaryAnalyzer;
    private EmotionAnalyzer fallbackAnalyzer;
    private CrisisFilter crisisFilter;
    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        primaryAnalyzer = mock(EmotionAnalyzer.class);
        fallbackAnalyzer = mock(EmotionAnalyzer.class);
        crisisFilter = mock(CrisisFilter.class);
        given(primaryAnalyzer.providerName()).willReturn("CLAUDE");
        given(fallbackAnalyzer.providerName()).willReturn("STATIC");
        service = new AiAnalysisService(List.of(primaryAnalyzer, fallbackAnalyzer), crisisFilter);
    }

    @Test
    void 정상적인_분석은_첫번째_analyzer를_사용한다() {
        PostContent content = new PostContent(1L, "요즘 너무 불안해요");
        EmotionAnalysisResult expected =
                new EmotionAnalysisResult(EmotionType.ANXIETY, 30, 0.9, "불안 표현");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(primaryAnalyzer.analyze(content)).willReturn(expected);

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.emotionType()).isEqualTo("ANXIETY");
        assertThat(response.hp()).isEqualTo(30);
        assertThat(response.usedProvider()).isEqualTo("CLAUDE");
        assertThat(response.crisisDetected()).isFalse();
        verify(fallbackAnalyzer, never()).analyze(any());
    }

    @Test
    void 위기_감지시_AI분석을_건너뛴다() {
        PostContent content = new PostContent(1L, "죽고 싶어요");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.crisis("죽고 싶"));

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.crisisDetected()).isTrue();
        verify(primaryAnalyzer, never()).analyze(any());
        verify(fallbackAnalyzer, never()).analyze(any());
    }

    @Test
    void 첫번째_analyzer_실패시_다음으로_폴백한다() {
        PostContent content = new PostContent(1L, "힘들어요");
        EmotionAnalysisResult fallbackResult =
                new EmotionAnalysisResult(EmotionType.LETHARGY, 10, 0.0, "fallback");
        given(crisisFilter.check(content.text())).willReturn(CrisisDetectionResult.safe());
        given(primaryAnalyzer.analyze(content)).willThrow(new RuntimeException("AI 서비스 오류"));
        given(fallbackAnalyzer.analyze(content)).willReturn(fallbackResult);

        AiAnalysisResponse response = service.analyze(content);

        assertThat(response.usedProvider()).isEqualTo("STATIC");
    }
}
