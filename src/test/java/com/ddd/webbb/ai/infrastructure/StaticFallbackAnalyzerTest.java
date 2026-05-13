package com.ddd.webbb.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.infrastructure.adapter.StaticFallbackAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticFallbackAnalyzerTest {

    private StaticFallbackAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new StaticFallbackAnalyzer();
    }

    @Test
    void 항상_기본값을_반환한다() {
        PostContent content = new PostContent(1L, "어떤 내용이든");

        EmotionAnalysisResult result = analyzer.analyze(content);

        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void providerName은_STATIC이다() {
        assertThat(analyzer.providerName()).isEqualTo("STATIC");
    }

    @Test
    void null_게시글도_기본값을_반환한다() {
        PostContent content = new PostContent(null, null);

        EmotionAnalysisResult result = analyzer.analyze(content);

        assertThat(result.isValid()).isTrue();
    }
}
