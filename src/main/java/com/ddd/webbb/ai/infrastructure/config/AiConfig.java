package com.ddd.webbb.ai.infrastructure.config;

import com.ddd.webbb.ai.infrastructure.adapter.ClaudeEmotionAnalyzer;
import com.ddd.webbb.ai.infrastructure.adapter.OpenAiEmotionAnalyzer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

@AutoConfiguration(
        after = {AnthropicChatAutoConfiguration.class, OpenAiChatAutoConfiguration.class})
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    @Order(1)
    @ConditionalOnBean(AnthropicChatModel.class)
    public ClaudeEmotionAnalyzer claudeEmotionAnalyzer(
            AnthropicChatModel model, AiProperties properties) throws IOException {
        String template = loadPromptTemplate(properties.promptVersion());
        return new ClaudeEmotionAnalyzer(ChatClient.builder(model).build(), template);
    }

    @Bean
    @Order(2)
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiEmotionAnalyzer openAiEmotionAnalyzer(
            OpenAiChatModel model, AiProperties properties) throws IOException {
        String template = loadPromptTemplate(properties.promptVersion());
        return new OpenAiEmotionAnalyzer(ChatClient.builder(model).build(), template);
    }

    private String loadPromptTemplate(String version) throws IOException {
        ClassPathResource resource =
                new ClassPathResource("prompts/emotion-analysis-" + version + ".st");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
