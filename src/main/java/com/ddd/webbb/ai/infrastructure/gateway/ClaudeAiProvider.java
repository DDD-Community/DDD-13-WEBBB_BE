package com.ddd.webbb.ai.infrastructure.gateway;

import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

public class ClaudeAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiProvider.class);

    private final ChatClient chatClient;

    public ClaudeAiProvider(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    @Retry(name = "claudeProvider")
    @CircuitBreaker(name = "claudeProvider")
    public String call(String prompt) {
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.warn("Claude API call failed: {}", e.getMessage());
            throw new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "Claude API 호출 실패", e);
        }
    }

    @Override
    public String providerName() {
        return "CLAUDE";
    }
}
