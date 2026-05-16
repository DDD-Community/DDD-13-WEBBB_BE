package com.ddd.webbb.ai.infrastructure.gateway;

import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

public class OpenAiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAiProvider.class);

    private final ChatClient chatClient;

    public OpenAiAiProvider(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    @Retry(name = "openAiProvider")
    @CircuitBreaker(name = "openAiProvider")
    public String call(String prompt) {
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.warn("OpenAI API call failed: {}", e.getMessage());
            throw new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "OpenAI API 호출 실패", e);
        }
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }
}
