package com.ddd.webbb.ai.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.ai.infrastructure.gateway.OpenAiAiProvider;
import com.ddd.webbb.ai.infrastructure.gateway.StaticAiProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(AiConfig.class))
                    .withPropertyValues("app.ai.prompt-version=v1", "app.ai.timeout=5s");

    private final ApplicationContextRunner autoConfigContextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(OpenAiChatAutoConfiguration.class, AiConfig.class))
                    .withPropertyValues(
                            "app.ai.prompt-version=v1",
                            "app.ai.timeout=5s",
                            "spring.ai.openai.api-key=test-dummy-key");

    @Test
    void OpenAI_모델이_있으면_OpenAI와_Static만_등록된다() {
        contextRunner
                .withBean(OpenAiChatModel.class, () -> Mockito.mock(OpenAiChatModel.class))
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(OpenAiAiProvider.class);
                            assertThat(context).hasSingleBean(StaticAiProvider.class);
                        });
    }

    @Test
    void OpenAI_모델이_없으면_Static만_남는다() {
        contextRunner.run(
                context -> {
                    assertThat(context).doesNotHaveBean(OpenAiAiProvider.class);
                    assertThat(context).hasSingleBean(StaticAiProvider.class);
                });
    }

    @Test
    void OpenAi_자동설정_이후에_provider가_등록된다() {
        autoConfigContextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(OpenAiChatModel.class);
                    assertThat(context).hasSingleBean(OpenAiAiProvider.class);
                    assertThat(context).hasSingleBean(StaticAiProvider.class);
                });
    }
}
