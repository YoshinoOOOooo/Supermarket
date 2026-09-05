package com.supermarket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/** 全局 REST 异常处理器，负责将异常转换为统一错误响应。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid request"
                : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return response(ErrorCode.INVALID_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, BindException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception) {
        return response(ErrorCode.INVALID_REQUEST, "Invalid request", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return response(exception.getErrorCode(), exception.getMessage(), statusFor(exception.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(ErrorCode.INTERNAL_ERROR, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus statusFor(ErrorCode code) {
        switch (code) {
            case PRODUCT_NOT_FOUND:
            case ORDER_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case PRODUCT_DISABLED:
            case PROMOTION_CONFLICT:
            case RESOURCE_CONFLICT:
            case INVALID_ORDER_STATE:
                return HttpStatus.CONFLICT;
            case INTERNAL_ERROR:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_REQUEST:
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }

    private ResponseEntity<ApiError> response(ErrorCode code, String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new ApiError(code.name(), message, LocalDateTime.now()));
    }
}
