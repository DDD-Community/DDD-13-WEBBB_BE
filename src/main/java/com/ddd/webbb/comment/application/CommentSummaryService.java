package com.ddd.webbb.comment.application;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.CommentSummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CommentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentSummaryService.class);
    private static final CommentSummaryResult FALLBACK =
            new CommentSummaryResult("요약 실패", "NEUTRAL");

    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public CommentSummaryService(
            AiGateway aiGateway,
            ObjectMapper objectMapper,
            @Qualifier("commentSummaryPromptTemplate") String promptTemplate) {
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptTemplate;
    }

    public CommentSummaryResponse summarize(Long commentId, String commentText) {
        String prompt = promptTemplate.replace("{content}", commentText);
        AiGatewayResult gatewayResult = aiGateway.call(prompt);
        CommentSummaryResult result = parseResponse(commentId, gatewayResult.rawResponse());
        return new CommentSummaryResponse(
                result.summary(), result.tone(), gatewayResult.providerName());
    }

    private CommentSummaryResult parseResponse(Long commentId, String json) {
        try {
            CommentSummaryResult result =
                    objectMapper.readValue(json.trim(), CommentSummaryResult.class);
            if (result.isValid()) {
                return result;
            }
            log.warn("[CommentSummary] 유효하지 않은 응답 commentId={}: {}", commentId, json);
        } catch (Exception e) {
            log.warn("[CommentSummary] 파싱 실패 commentId={}: {}", commentId, json);
        }
        return FALLBACK;
    }
}
