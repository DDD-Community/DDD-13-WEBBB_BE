package com.dnd.poc.retrospective.domain.exception;

import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;

public class RetryableUpstreamException extends RetrospectiveGenerationException {
    public RetryableUpstreamException(ErrorCode code, String detail, Throwable cause) {
        super(code, detail, cause);
    }
}
