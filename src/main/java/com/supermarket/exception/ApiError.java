package com.supermarket.exception;

import java.time.LocalDateTime;

/** API 统一错误响应对象。 */
public final class ApiError {
    /** 机器可识别的标准错误码。 */
    private final String code;
    /** 供调用方阅读的错误说明。 */
    private final String message;
    /** 错误响应生成时间。 */
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
