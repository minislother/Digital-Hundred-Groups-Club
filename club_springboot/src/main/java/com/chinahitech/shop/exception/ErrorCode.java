package com.chinahitech.shop.exception;

/**
 * 后端统一异常响应使用的错误码常量。
 */
public final class ErrorCode {

    public static final String PARAM_ERROR = "PARAM_ERROR";
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String RATE_LIMIT = "RATE_LIMIT";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ErrorCode() {
    }
}
