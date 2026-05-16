package com.ddd.webbb.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.infrastructure.gateway.StaticAiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticAiProviderTest {

    private StaticAiProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        provider = new StaticAiProvider();
        objectMapper = new ObjectMapper();
    }

    @Test
    void 항상_비어있지_않은_문자열을_반환한다() {
        String result = provider.call("어떤 프롬프트든");
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void 반환값은_파싱_가능한_EmotionAnalysisResult_JSON이다() throws Exception {
        String json = provider.call("테스트");
        EmotionAnalysisResult result = objectMapper.readValue(json, EmotionAnalysisResult.class);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void providerName은_STATIC이다() {
        assertThat(provider.providerName()).isEqualTo("STATIC");
    }

    @Test
    void 예외를_던지지_않는다() {
        assertThatCode(() -> provider.call(null)).doesNotThrowAnyException();
    }
}
