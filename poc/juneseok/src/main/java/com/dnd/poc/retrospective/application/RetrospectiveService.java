package com.dnd.poc.retrospective.application;

import com.dnd.poc.retrospective.application.port.KptAnalyzer;
import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import com.dnd.poc.retrospective.domain.RetrospectiveResult;
import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;
import com.dnd.poc.retrospective.domain.exception.RetrospectiveGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RetrospectiveService {

    private static final Logger log = LoggerFactory.getLogger(RetrospectiveService.class);

    private final KptAnalyzer analyzer;

    public RetrospectiveService(KptAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public RetrospectiveResult analyze(RetrospectiveContext context) {
        log.info("Retrospective analyze requested. length={}", context.length());
        try {
            KptAnalysis analysis = analyzer.analyze(context);
            if (analysis.isEmpty()) {
                return new RetrospectiveResult.Failure(ErrorCode.EMPTY_RESPONSE, "AI returned empty analysis");
            }
            return new RetrospectiveResult.Success(analysis);
        } catch (RetrospectiveGenerationException e) {
            log.warn("AI analyze failed code={} detail={}", e.code(), e.getMessage());
            return new RetrospectiveResult.Failure(e.code(), e.getMessage());
        }
    }
}
