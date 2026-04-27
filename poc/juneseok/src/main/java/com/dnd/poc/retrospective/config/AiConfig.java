package com.dnd.poc.retrospective.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    public static final String LIVE_KEY_CONDITION =
            "'${spring.ai.openai.api-key:}'.trim().startsWith('sk-')";

    public static final String MOCK_KEY_CONDITION =
            "!('${spring.ai.openai.api-key:}'.trim().startsWith('sk-'))";

    @Bean
    @ConditionalOnExpression(LIVE_KEY_CONDITION)
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnExpression(LIVE_KEY_CONDITION)
    public RestClientCustomizer aiHttpTimeoutCustomizer(AiProperties props) {
        Duration timeout = props.timeout();
        return restClient -> {
            HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(timeout);
            restClient.requestFactory(factory);
        };
    }

    @Bean
    @ConditionalOnExpression(LIVE_KEY_CONDITION)
    public KptSystemPrompt kptSystemPrompt(AiProperties props, ResourceLoader loader) throws IOException {
        Resource resource = loader.getResource(props.promptLocation());
        try (var in = resource.getInputStream()) {
            return new KptSystemPrompt(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public record KptSystemPrompt(String value) {}
}
