package com.dnd.poc.sentiment.application;

import com.dnd.poc.global.common.exception.AppException;
import com.dnd.poc.global.common.exception.ErrorCode;
import com.dnd.poc.sentiment.domain.SentimentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class SentimentService {

    private static final Logger log = LoggerFactory.getLogger(SentimentService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String promptVersion;
    private final boolean logText;

    public SentimentService(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            @Value("${app.prompt.version:v1}") String promptVersion,
            @Value("${app.logging.log-text:false}") boolean logText) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.promptVersion = promptVersion;
        this.logText = logText;
    }

    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new AppException(ErrorCode.EMPTY_TEXT);
        }

        MDC.put("textLength", String.valueOf(text.length()));
        MDC.put("promptVersion", promptVersion);

        long start = System.currentTimeMillis();
        String rawPrompt = loadPromptTemplate(promptVersion);
        String systemPrompt = rawPrompt.replace("{text}", text);

        SentimentResult result = callWithRetry(systemPrompt, text, 1);

        long elapsed = System.currentTimeMillis() - start;
        MDC.put("label", result.label().name());
        MDC.put("score", String.valueOf(result.score()));
        MDC.put("processingTimeMs", String.valueOf(elapsed));

        if (logText) {
            log.info(
                    "sentiment.analyze.success | text=\"{}\" label={} score={} confidence={} processingTimeMs={}",
                    text,
                    result.label(),
                    result.score(),
                    result.confidence(),
                    elapsed);
        } else {
            log.info(
                    "sentiment.analyze.success | label={} score={} confidence={} processingTimeMs={}",
                    result.label(),
                    result.score(),
                    result.confidence(),
                    elapsed);
        }

        return result;
    }

    private SentimentResult callWithRetry(String systemPrompt, String userText, int attempt) {
        try {
            String raw = chatClient.prompt().system(systemPrompt).user(userText).call().content();
            return parse(raw);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            if (attempt < 2) {
                log.warn(
                        "sentiment.parse.retry | attempt={} reason={}",
                        attempt + 1,
                        e.getMessage());
                return callWithRetry(systemPrompt, userText, attempt + 1);
            }
            log.error("sentiment.analyze.failed | errorCode={}", ErrorCode.SENTIMENT_PARSE_ERROR);
            throw new AppException(ErrorCode.SENTIMENT_PARSE_ERROR);
        }
    }

    private SentimentResult parse(String raw) {
        try {
            String json = raw.trim();
            // LLM이 코드블록으로 감쌀 경우 제거
            if (json.startsWith("```")) {
                json = json.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }
            return objectMapper.readValue(json, SentimentResult.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed: " + e.getMessage(), e);
        }
    }

    private String loadPromptTemplate(String version) {
        try {
            ClassPathResource resource =
                    new ClassPathResource("prompts/sentiment-" + version + ".st");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
