package com.chinahitech.shop.exception;

/**
 * 通用业务异常，用于表达参数合法但业务规则不允许继续执行的场景。
 */
public class BusinessException extends ServiceException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(String code, String message) {
        super(code, message);
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
