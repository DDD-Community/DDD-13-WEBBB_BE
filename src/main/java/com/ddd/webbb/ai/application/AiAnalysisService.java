package com.ddd.webbb.ai.application;

import com.ddd.webbb.ai.domain.CrisisDetectionResult;
import com.ddd.webbb.ai.domain.CrisisFilter;
import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.EmotionAnalyzer;
import com.ddd.webbb.ai.domain.PostContent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final List<EmotionAnalyzer> analyzers;
    private final CrisisFilter crisisFilter;

    public AiAnalysisService(List<EmotionAnalyzer> analyzers, CrisisFilter crisisFilter) {
        this.analyzers = analyzers;
        this.crisisFilter = crisisFilter;
    }

    public AiAnalysisResponse analyze(PostContent content) {
        CrisisDetectionResult crisis = crisisFilter.check(content.text());
        if (crisis.isCrisis()) {
            return crisisResponse(crisis);
        }
        return runAnalyzers(content);
    }

    private AiAnalysisResponse runAnalyzers(PostContent content) {
        for (EmotionAnalyzer analyzer : analyzers) {
            try {
                EmotionAnalysisResult result = analyzer.analyze(content);
                if (result.isValid()) {
                    return toResponse(result, analyzer.providerName(), false);
                }
            } catch (Exception e) {
                log.warn("AI analyzer [{}] failed: {}", analyzer.providerName(), e.getMessage());
            }
        }
        EmotionAnalysisResult fallback = EmotionAnalysisResult.safeDefault();
        return toResponse(fallback, "STATIC", false);
    }

    private AiAnalysisResponse toResponse(EmotionAnalysisResult result, String provider, boolean crisisDetected) {
        return new AiAnalysisResponse(
            result.emotionType().name(),
            result.hp(),
            result.confidence(),
            result.reason(),
            crisisDetected,
            provider
        );
    }

    private AiAnalysisResponse crisisResponse(CrisisDetectionResult crisis) {
        EmotionAnalysisResult fallback = EmotionAnalysisResult.safeDefault();
        return new AiAnalysisResponse(
            fallback.emotionType().name(),
            fallback.hp(),
            fallback.confidence(),
            "위기 감지: " + crisis.matchedKeyword(),
            true,
            "CRISIS_FILTER"
        );
    }
}
