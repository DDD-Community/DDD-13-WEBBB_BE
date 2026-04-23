package com.dnd.webbb.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.webbb.ai.application.dto.AnalysisResponse;
import com.dnd.webbb.ai.config.AiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CLAUDE_API_KEY", matches = ".+")
class AiEncouragementServiceIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.dnd.webbb.ai")
    @Import(AiConfig.class)
    static class TestApp {}

    @Autowired private AiEncouragementService service;

    @Test
    void 정상_고민글에_대해_AI_응답을_정상_수신한다() {
        AnalysisResponse result = service.analyze("면접을 10번이나 떨어졌어. 이제 자신감이 하나도 없어.", "친구같은");

        assertThat(result.crisisDetected()).isFalse();
        assertThat(result.emotionTags()).isNotEmpty();
        assertThat(result.summary()).isNotBlank();
        assertThat(result.encouragementMessage()).isNotBlank();
        assertThat(result.smallActionTip()).isNotBlank();
    }

    @Test
    void 위기_키워드_포함시_AI_호출_없이_안전_안내를_반환한다() {
        AnalysisResponse result = service.analyze("취업이 안돼서 죽고 싶어", "친구같은");

        assertThat(result.crisisDetected()).isTrue();
        assertThat(result.encouragementMessage()).contains("1393");
        assertThat(result.encouragementMessage()).contains("109");
        assertThat(result.emotionTags()).isEmpty();
    }
}
