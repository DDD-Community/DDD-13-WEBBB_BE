package com.ddd.webbb.ai.domain.exception;

public class RetryableAiException extends RuntimeException {

    private final AiErrorCode errorCode;

    public RetryableAiException(AiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RetryableAiException(AiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AiErrorCode getErrorCode() {
        return errorCode;
    }
}
