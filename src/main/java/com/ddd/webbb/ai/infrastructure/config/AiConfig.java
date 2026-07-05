package com.ddd.webbb.ai.infrastructure.config;

import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.infrastructure.gateway.OpenAiAiProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

@AutoConfiguration(after = OpenAiChatAutoConfiguration.class)
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    @Order(1)
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiAiProvider openAiAiProvider(OpenAiChatModel model) {
        return new OpenAiAiProvider(ChatClient.builder(model).build());
    }

    @Bean
    public CrisisFilter crisisFilter() {
        return new CrisisFilter();
    }

    @Bean
    @Qualifier("emotionPromptTemplate")
    public String emotionPromptTemplate(AiProperties properties) throws IOException {
        ClassPathResource resource =
                new ClassPathResource(
                        "prompts/emotion-analysis-" + properties.promptVersion() + ".st");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Bean
    @Qualifier("commentSummaryPromptTemplate")
    public String commentSummaryPromptTemplate(AiProperties properties) throws IOException {
        ClassPathResource resource =
                new ClassPathResource(
                        "prompts/comment-summary-" + properties.promptVersion() + ".st");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
