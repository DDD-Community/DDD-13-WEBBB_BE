package com.dnd.poc.retrospective.infrastructure.ai;

import com.dnd.poc.retrospective.application.port.KptAnalyzer;
import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import com.dnd.poc.retrospective.domain.exception.RetryableUpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 다중 모델 안정성을 위한 Fallback 어댑터입니다.
 * 실서비스 시나리오에서 주 모델(OpenAI) 장애 시 보조 모델이나 Mock으로 전환하여 서비스 가용성을 보장합니다.
 */
@Primary
@Component
class FallbackKptAnalyzer implements KptAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(FallbackKptAnalyzer.class);
    
    private final List<KptAnalyzer> analyzers;

    FallbackKptAnalyzer(List<KptAnalyzer> analyzers) {
        // FallbackKptAnalyzer 자체를 제외한 나머지 분석기들을 수집
        this.analyzers = analyzers.stream()
                .filter(a -> a != this)
                .toList();
    }

    @Override
    public KptAnalysis analyze(RetrospectiveContext context) {
        RetryableUpstreamException lastException = null;

        for (KptAnalyzer analyzer : analyzers) {
            try {
                log.info("Attempting analysis with: {}", analyzer.getClass().getSimpleName());
                return analyzer.analyze(context);
            } catch (RetryableUpstreamException e) {
                log.warn("Analyzer {} failed, trying next. error={}", 
                        analyzer.getClass().getSimpleName(), e.getMessage());
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new IllegalStateException("No KptAnalyzers available");
    }
}
