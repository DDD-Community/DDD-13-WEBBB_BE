package com.ddd.webbb.ai.infrastructure.observability;

import com.ddd.webbb.ai.domain.AiMetricsRecorder;
import com.ddd.webbb.ai.domain.AiMetricsTags;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiMetricsLogger implements AiMetricsRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiMetricsLogger.class);
    private static final String TIMER_NAME = "ai.emotion.analyze.duration";

    private final MeterRegistry meterRegistry;

    @Override
    public <T> T recordAndLog(
            Long postId, Supplier<T> action, Function<T, AiMetricsTags> tagExtractor) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T response = action.get();
            AiMetricsTags tags = tagExtractor.apply(response);
            sample.stop(
                    Timer.builder(TIMER_NAME)
                            .tag("provider", tags.provider())
                            .tag("emotion", tags.emotion())
                            .tag("crisis", String.valueOf(tags.crisis()))
                            .register(meterRegistry));
            log.info(
                    "[AI] postId={} emotion={} hp={} provider={} crisis={}",
                    postId,
                    tags.emotion(),
                    tags.hp(),
                    tags.provider(),
                    tags.crisis());
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
