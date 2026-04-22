package com.dnd.webbb.global.common.exception;

import com.dnd.webbb.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ApiResponse.FieldError> errors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(
                                fe ->
                                        new ApiResponse.FieldError(
                                                fe.getField(), fe.getDefaultMessage()))
                        .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getMessage(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException e) {
        List<ApiResponse.FieldError> errors =
                e.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new ApiResponse.FieldError(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()))
                        .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getMessage(), errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {
        return ResponseEntity.status(ErrorCode.DUPLICATED_EMAIL.getStatus())
                .body(ApiResponse.fail(ErrorCode.DUPLICATED_EMAIL.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
