package com.dnd.webbb.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success, String message, T data, List<FieldError> errors, LocalDateTime timestamp) {

    public record FieldError(String field, String reason) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "요청이 성공했습니다.", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String message, List<FieldError> errors) {
        return new ApiResponse<>(false, message, null, errors, LocalDateTime.now());
    }
}
