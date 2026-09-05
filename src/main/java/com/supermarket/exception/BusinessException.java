package com.supermarket.exception;

/** 可预期的业务异常，由全局异常处理器转换为标准 API 错误响应。 */
public class BusinessException extends RuntimeException {
    /** 与本次业务失败对应的标准错误码。 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
