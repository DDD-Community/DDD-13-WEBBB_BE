package com.ddd.webbb.ai.application;

import com.ddd.webbb.ai.domain.AiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);

    private final ObjectMapper objectMapper;

    public <T extends AiResult> T parse(String rawResponse, Class<T> type, Supplier<T> fallback) {
        try {
            T result = objectMapper.readValue(stripCodeFence(rawResponse), type);
            if (result.isValid()) {
                return result;
            }
            log.warn("[AI] 유효하지 않은 응답, 기본값 사용: {}", rawResponse);
        } catch (Exception e) {
            log.warn("[AI] 응답 파싱 실패, 기본값 사용: {}", rawResponse);
        }
        return fallback.get();
    }

    private String stripCodeFence(String rawResponse) {
        String text = rawResponse.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd < 0) {
            return text;
        }
        text = text.substring(firstLineEnd + 1);
        int closingFence = text.lastIndexOf("```");
        if (closingFence >= 0) {
            text = text.substring(0, closingFence);
        }
        return text.trim();
    }
}
