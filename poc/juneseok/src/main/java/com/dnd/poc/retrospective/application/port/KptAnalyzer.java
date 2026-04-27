package com.dnd.poc.retrospective.application.port;

import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;

public interface KptAnalyzer {
    KptAnalysis analyze(RetrospectiveContext context);

    default boolean isMock() {
        return false;
    }
}
