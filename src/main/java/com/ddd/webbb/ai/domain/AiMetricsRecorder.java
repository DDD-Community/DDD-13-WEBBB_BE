package com.ddd.webbb.ai.domain;

import java.util.function.Function;
import java.util.function.Supplier;

public interface AiMetricsRecorder {

    <T> T recordAndLog(Long postId, Supplier<T> action, Function<T, AiMetricsTags> tagExtractor);
}
