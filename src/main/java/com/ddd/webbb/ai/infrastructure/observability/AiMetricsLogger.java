package com.ddd.webbb.ai.infrastructure.observability;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiMetricsLogger {

    private static final Logger log = LoggerFactory.getLogger(AiMetricsLogger.class);
    private static final String TIMER_NAME = "ai.emotion.analyze.duration";

    private final MeterRegistry meterRegistry;

    public AiMetricsLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public AiAnalysisResponse recordAndLog(Long postId, Supplier<AiAnalysisResponse> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            AiAnalysisResponse response = action.get();
            sample.stop(
                    Timer.builder(TIMER_NAME)
                            .tag("provider", response.usedProvider())
                            .tag("emotion", response.emotionType())
                            .tag("crisis", String.valueOf(response.crisisDetected()))
                            .register(meterRegistry));
            log.info(
                    "[AI] postId={} emotion={} hp={} provider={} crisis={}",
                    postId,
                    response.emotionType(),
                    response.hp(),
                    response.usedProvider(),
                    response.crisisDetected());
            return response;
        } catch (Exception e) {
            sample.stop(
                    Timer.builder(TIMER_NAME)
                            .tag("provider", "ERROR")
                            .tag("emotion", "UNKNOWN")
                            .tag("crisis", "false")
                            .register(meterRegistry));
            throw e;
        }
    }
}
