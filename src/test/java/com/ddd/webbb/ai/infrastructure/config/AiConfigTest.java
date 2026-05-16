package com.ddd.webbb.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.ai.infrastructure.gateway.ClaudeAiProvider;
import com.ddd.webbb.ai.infrastructure.gateway.OpenAiAiProvider;
import com.ddd.webbb.ai.infrastructure.gateway.StaticAiProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(AiConfig.class))
                    .withPropertyValues("app.ai.prompt-version=v1", "app.ai.timeout=5s");

    @Test
    void 기본값에서는_Claude는_비활성화되고_OpenAI와_Static만_등록된다() {
        contextRunner
                .withBean(AnthropicChatModel.class, () -> Mockito.mock(AnthropicChatModel.class))
                .withBean(OpenAiChatModel.class, () -> Mockito.mock(OpenAiChatModel.class))
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ClaudeAiProvider.class);
                            assertThat(context).hasSingleBean(OpenAiAiProvider.class);
                            assertThat(context).hasSingleBean(StaticAiProvider.class);
                        });
    }

    @Test
    void Claude를_명시적으로_활성화하면_Claude도_등록된다() {
        contextRunner
                .withPropertyValues("app.ai.providers.claude-enabled=true")
                .withBean(AnthropicChatModel.class, () -> Mockito.mock(AnthropicChatModel.class))
                .withBean(OpenAiChatModel.class, () -> Mockito.mock(OpenAiChatModel.class))
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ClaudeAiProvider.class);
                            assertThat(context).hasSingleBean(OpenAiAiProvider.class);
                            assertThat(context).hasSingleBean(StaticAiProvider.class);
                        });
    }

    @Test
    void OpenAI를_비활성화하면_Static만_남는다() {
        contextRunner
                .withPropertyValues("app.ai.providers.openai-enabled=false")
                .withBean(OpenAiChatModel.class, () -> Mockito.mock(OpenAiChatModel.class))
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ClaudeAiProvider.class);
                            assertThat(context).doesNotHaveBean(OpenAiAiProvider.class);
                            assertThat(context).hasSingleBean(StaticAiProvider.class);
                        });
    }
}
