package com.ddd.webbb.ai.domain.exception;

public class PermanentAiException extends RuntimeException {

    private final AiErrorCode errorCode;

    public PermanentAiException(AiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PermanentAiException(AiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AiErrorCode getErrorCode() {
        return errorCode;
    }
}
