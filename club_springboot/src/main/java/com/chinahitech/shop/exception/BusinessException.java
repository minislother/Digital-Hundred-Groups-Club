package com.chinahitech.shop.exception;

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
