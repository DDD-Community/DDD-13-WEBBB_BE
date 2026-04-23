package com.dnd.poc.global.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMPTY_TEXT(HttpStatus.BAD_REQUEST, "분석할 텍스트를 입력해주세요."),
    CLOVA_API_ERROR(HttpStatus.BAD_GATEWAY, "CLOVA API 호출에 실패했습니다."),
    OPENAI_API_ERROR(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다."),
    SENTIMENT_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "감성 분석 결과 파싱에 실패했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
