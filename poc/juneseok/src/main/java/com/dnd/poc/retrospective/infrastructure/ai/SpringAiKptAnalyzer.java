package com.dnd.poc.retrospective.infrastructure.ai;

import com.dnd.poc.retrospective.application.port.KptAnalyzer;
import com.dnd.poc.retrospective.config.AiConfig.KptSystemPrompt;
import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;
import com.dnd.poc.retrospective.domain.exception.PermanentResponseException;
import com.dnd.poc.retrospective.domain.exception.RetrospectiveGenerationException;
import com.dnd.poc.retrospective.domain.exception.RetryableUpstreamException;
import com.fasterxml.jackson.core.JacksonException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dnd.poc.retrospective.config.AiConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnExpression(AiConfig.LIVE_KEY_CONDITION)
class SpringAiKptAnalyzer implements KptAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(SpringAiKptAnalyzer.class);
    private static final String RESILIENCE_NAME = "kptAnalyzer";

    private final ChatClient chatClient;
    private final KptSystemPrompt systemPrompt;

    SpringAiKptAnalyzer(ChatClient chatClient, KptSystemPrompt systemPrompt) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
    }

    @Override
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "onCircuitOpen")
    public KptAnalysis analyze(RetrospectiveContext context) {
        return toDomain(callLlm(context));
    }

    private KptResponse callLlm(RetrospectiveContext context) {
        try {
            return chatClient
                    .prompt()
                    .system(systemPrompt.value())
                    .user(context.value())
                    .call()
                    .entity(KptResponse.class);
        } catch (RetrospectiveGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw classify(e);
        }
    }

    private static RetrospectiveGenerationException classify(Exception e) {
        Throwable c = rootCause(e);
        if (c instanceof JacksonException) {
            return new PermanentResponseException(
                    ErrorCode.INVALID_RESPONSE, "AI response unparsable: " + c.getMessage(), e);
        }
        if (c instanceof TimeoutException
                || c instanceof HttpTimeoutException
                || c instanceof SocketTimeoutException) {
            return new RetryableUpstreamException(
                    ErrorCode.UPSTREAM_TIMEOUT, "LLM call timed out: " + c.getMessage(), e);
        }
        if (c instanceof IOException || c instanceof RestClientException) {
            return new RetryableUpstreamException(
                    ErrorCode.UPSTREAM_UNAVAILABLE, "LLM call failed: " + c.getMessage(), e);
        }
        return new RetryableUpstreamException(
                ErrorCode.UPSTREAM_UNAVAILABLE, "LLM call failed: " + e.getMessage(), e);
    }

    private static Throwable rootCause(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c;
    }

    private KptAnalysis toDomain(KptResponse response) {
        if (response == null) {
            throw new PermanentResponseException(ErrorCode.EMPTY_RESPONSE, "AI returned null");
        }
        try {
            return new KptAnalysis(
                    response.keep(),
                    response.problem(),
                    response.tries(),
                    response.cheerMessage()
            );
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new PermanentResponseException(
                    ErrorCode.INVALID_RESPONSE, "AI response invalid: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private KptAnalysis onCircuitOpen(RetrospectiveContext context, Throwable t) {
        log.warn("Circuit breaker fallback. cause={}", t.toString());
        if (t instanceof RetrospectiveGenerationException rge) {
            throw rge;
        }
        throw new RetryableUpstreamException(
                ErrorCode.UPSTREAM_UNAVAILABLE, "Circuit open: " + t.getMessage(), t);
    }
}
