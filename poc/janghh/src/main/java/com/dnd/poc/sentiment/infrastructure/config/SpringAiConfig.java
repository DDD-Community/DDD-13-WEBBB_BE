package com.dnd.poc.sentiment.infrastructure.config;

import com.dnd.poc.sentiment.infrastructure.clova.ClovaChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "clova", matchIfMissing = true)
    public ChatClient clovaChatClient(ClovaChatModel clovaChatModel) {
        return ChatClient.builder(clovaChatModel).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
    public ChatClient openaiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
