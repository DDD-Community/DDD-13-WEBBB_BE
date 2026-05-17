package com.ddd.webbb.ai.application;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.PermanentAiException;
import com.ddd.webbb.ai.infrastructure.observability.AiMetricsLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final AiGateway aiGateway;
    private final CrisisFilter crisisFilter;
    private final AiMetricsLogger metricsLogger;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public AiAnalysisService(
            AiGateway aiGateway,
            CrisisFilter crisisFilter,
            AiMetricsLogger metricsLogger,
            ObjectMapper objectMapper,
            @Qualifier("emotionPromptTemplate") String promptTemplate) {
        this.aiGateway = aiGateway;
        this.crisisFilter = crisisFilter;
        this.metricsLogger = metricsLogger;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptTemplate;
    }

    public AiAnalysisResponse analyze(PostContent content) {
        CrisisDetectionResult crisis = crisisFilter.check(content.text());
        if (crisis.isCrisis()) {
            return crisisResponse(crisis);
        }
        return metricsLogger.recordAndLog(content.postId(), () -> doAnalyze(content));
    }

    private AiAnalysisResponse doAnalyze(PostContent content) {
        String prompt = promptTemplate.replace("{content}", content.text());
        AiGatewayResult gatewayResult = aiGateway.call(prompt);
        EmotionAnalysisResult result = parseResponse(gatewayResult.rawResponse());
        return toResponse(result, gatewayResult.providerName(), false);
    }

    private EmotionAnalysisResult parseResponse(String json) {
        try {
            EmotionAnalysisResult result =
                    objectMapper.readValue(json.trim(), EmotionAnalysisResult.class);
            if (!result.isValid()) {
                throw new PermanentAiException(
                        AiErrorCode.INVALID_RESPONSE, "유효하지 않은 AI 응답: " + json);
            }
            return result;
        } catch (PermanentAiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, 기본값 사용: {}", json);
            return EmotionAnalysisResult.safeDefault();
        }
    }

    private AiAnalysisResponse toResponse(
            EmotionAnalysisResult result, String provider, boolean crisisDetected) {
        return new AiAnalysisResponse(
                result.emotionType().name(),
                result.hp(),
                result.confidence(),
                result.reason(),
                crisisDetected,
                provider);
    }

    private AiAnalysisResponse crisisResponse(CrisisDetectionResult crisis) {
        EmotionAnalysisResult fallback = EmotionAnalysisResult.safeDefault();
        return new AiAnalysisResponse(
                fallback.emotionType().name(),
                fallback.hp(),
                fallback.confidence(),
                "위기 감지: " + crisis.matchedKeyword(),
                true,
                "CRISIS_FILTER");
    }
}
