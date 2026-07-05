package com.ddd.webbb.comment.application;

import com.ddd.webbb.ai.application.AiResponseParser;
import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.CommentSummaryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CommentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentSummaryService.class);
    private static final String FALLBACK_PROVIDER = "STATIC";
    private static final CommentSummaryResult FALLBACK =
            new CommentSummaryResult("요약 실패", "NEUTRAL");

    private final AiGateway aiGateway;
    private final AiResponseParser responseParser;
    private final String promptTemplate;

    public CommentSummaryService(
            AiGateway aiGateway,
            AiResponseParser responseParser,
            @Qualifier("commentSummaryPromptTemplate") String promptTemplate) {
        this.aiGateway = aiGateway;
        this.responseParser = responseParser;
        this.promptTemplate = promptTemplate;
    }

    public CommentSummaryResponse summarize(Long commentId, String commentText) {
        String prompt = promptTemplate.replace("{content}", commentText);
        AiGatewayResult gatewayResult;
        try {
            gatewayResult = aiGateway.call(prompt);
        } catch (Exception e) {
            log.warn("[CommentSummary] 게이트웨이 호출 실패 commentId={}: {}", commentId, e.getMessage());
            return new CommentSummaryResponse(
                    FALLBACK.summary(), FALLBACK.tone(), FALLBACK_PROVIDER);
        }
        CommentSummaryResult result =
                responseParser.parse(
                        gatewayResult.rawResponse(), CommentSummaryResult.class, () -> FALLBACK);
        return new CommentSummaryResponse(
                result.summary(), result.tone(), gatewayResult.providerName());
    }
}
