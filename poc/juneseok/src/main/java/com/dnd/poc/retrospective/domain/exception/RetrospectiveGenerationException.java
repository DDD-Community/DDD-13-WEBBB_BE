package com.dnd.poc.retrospective.domain.exception;

import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;

public class RetrospectiveGenerationException extends RuntimeException {

    private final ErrorCode code;

    public RetrospectiveGenerationException(ErrorCode code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    public RetrospectiveGenerationException(ErrorCode code, String detail) {
        this(code, detail, null);
    }

    public ErrorCode code() {
        return code;
    }
}
