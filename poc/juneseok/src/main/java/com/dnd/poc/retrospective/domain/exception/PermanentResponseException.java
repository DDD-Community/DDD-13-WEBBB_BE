package com.dnd.poc.retrospective.domain.exception;

import com.dnd.poc.retrospective.domain.RetrospectiveResult.ErrorCode;

public class PermanentResponseException extends RetrospectiveGenerationException {
    public PermanentResponseException(ErrorCode code, String detail, Throwable cause) {
        super(code, detail, cause);
    }

    public PermanentResponseException(ErrorCode code, String detail) {
        super(code, detail, null);
    }
}
