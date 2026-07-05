package com.ddd.webbb.ai.application;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.AiMetricsRecorder;
import com.ddd.webbb.ai.domain.AiMetricsTags;
import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.PostContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final String FALLBACK_PROVIDER = "STATIC";

    private final AiGateway aiGateway;
    private final CrisisFilter crisisFilter;
    private final AiMetricsRecorder metricsRecorder;
    private final AiResponseParser responseParser;
    private final String promptTemplate;

    public AiAnalysisService(
            AiGateway aiGateway,
            CrisisFilter crisisFilter,
            AiMetricsRecorder metricsRecorder,
            AiResponseParser responseParser,
            @Qualifier("emotionPromptTemplate") String promptTemplate) {
        this.aiGateway = aiGateway;
        this.crisisFilter = crisisFilter;
        this.metricsRecorder = metricsRecorder;
        this.responseParser = responseParser;
        this.promptTemplate = promptTemplate;
    }

    public AiAnalysisResponse analyze(PostContent content) {
        CrisisDetectionResult crisis = crisisFilter.check(content.text());
        if (crisis.isCrisis()) {
            return crisisResponse(crisis);
        }
        return metricsRecorder.recordAndLog(
                content.postId(),
                () -> doAnalyze(content),
                response ->
                        new AiMetricsTags(
                                response.usedProvider(),
                                response.emotionType(),
                                response.hp(),
                                response.crisisDetected()));
    }

    private AiAnalysisResponse doAnalyze(PostContent content) {
        String prompt = promptTemplate.replace("{content}", content.text());
        AiGatewayResult gatewayResult;
        try {
            gatewayResult = aiGateway.call(prompt);
        } catch (Exception e) {
            log.warn("[AI] 게이트웨이 호출 실패, 기본값 사용: {}", e.getMessage());
            return toResponse(EmotionAnalysisResult.safeDefault(), FALLBACK_PROVIDER, false);
        }
        EmotionAnalysisResult result =
                responseParser.parse(
                        gatewayResult.rawResponse(),
                        EmotionAnalysisResult.class,
                        EmotionAnalysisResult::safeDefault);
        return toResponse(result, gatewayResult.providerName(), false);
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
