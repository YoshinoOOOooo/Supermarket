package com.supermarket.exception;

import java.time.LocalDateTime;

public final class ApiError {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    public ApiError(String code, String message, LocalDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
