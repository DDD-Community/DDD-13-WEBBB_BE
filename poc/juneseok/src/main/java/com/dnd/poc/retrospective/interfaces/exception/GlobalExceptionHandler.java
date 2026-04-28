package com.dnd.poc.retrospective.interfaces.exception;

import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;
import com.dnd.poc.retrospective.domain.exception.RetrospectiveGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI TYPE_RETROSPECTIVE = URI.create("urn:problem:retrospective");
    private static final URI TYPE_VALIDATION = URI.create("urn:problem:validation");

    @ExceptionHandler(RetrospectiveGenerationException.class)
    public ProblemDetail handleGeneration(RetrospectiveGenerationException e) {
        HttpStatus status = mapStatus(e.code());
        log.warn("retrospective failure: code={} status={}", e.code(), status);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, "AI 회고 생성에 실패했습니다.");
        pd.setTitle("Retrospective generation failed");
        pd.setType(TYPE_RETROSPECTIVE);
        pd.setProperty("errorCode", e.code().name());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation failed");
        pd.setType(TYPE_VALIDATION);
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Invalid request");
        pd.setType(TYPE_VALIDATION);
        return pd;
    }

    private static HttpStatus mapStatus(ErrorCode code) {
        return switch (code) {
            case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case EMPTY_RESPONSE, INVALID_RESPONSE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case FALLBACK_TRIGGERED -> HttpStatus.OK;
        };
    }
}
