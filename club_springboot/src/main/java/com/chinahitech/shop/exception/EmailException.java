package com.chinahitech.shop.exception;

/**
 * 邮箱格式、校园邮箱校验或邮件发送失败时抛出的业务异常。
 */
public class EmailException extends ServiceException {
    public EmailException() {
        super();
    }

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailException(Throwable cause) {
        super(cause);
    }

    protected EmailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
