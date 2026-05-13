package com.ddd.webbb.ai.infrastructure.adapter;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.EmotionAnalyzer;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.PermanentAiException;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

public class ClaudeEmotionAnalyzer implements EmotionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeEmotionAnalyzer.class);

    private final ChatClient chatClient;
    private final String promptTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeEmotionAnalyzer(ChatClient chatClient, String promptTemplate) {
        this.chatClient = chatClient;
        this.promptTemplate = promptTemplate;
    }

    @Override
    @Retry(name = "claudeAnalyzer")
    @CircuitBreaker(name = "claudeAnalyzer")
    public EmotionAnalysisResult analyze(PostContent content) {
        try {
            String prompt = promptTemplate.replace("{content}", content.text());
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            return parseResponse(response);
        } catch (PermanentAiException | RetryableAiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Claude API call failed: {}", e.getMessage());
            throw new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "Claude API 호출 실패", e);
        }
    }

    @Override
    public String providerName() {
        return "CLAUDE";
    }

    private EmotionAnalysisResult parseResponse(String json) {
        try {
            String trimmed = json.trim();
            EmotionAnalysisResult result = objectMapper.readValue(trimmed, EmotionAnalysisResult.class);
            if (!result.isValid()) {
                throw new PermanentAiException(AiErrorCode.INVALID_RESPONSE, "유효하지 않은 AI 응답: " + json);
            }
            return result;
        } catch (PermanentAiException e) {
            throw e;
        } catch (Exception e) {
            throw new PermanentAiException(AiErrorCode.INVALID_RESPONSE, "AI 응답 파싱 실패: " + json, e);
        }
    }
}
