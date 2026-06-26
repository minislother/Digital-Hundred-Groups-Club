package com.chinahitech.shop.exception;

/**
 * 申请提交或申请次数限制相关操作失败时抛出的业务异常。
 */
public class ApplyException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public ApplyException() {
        super();
    }

    public ApplyException(String message) {
        super(message);
    }

    public ApplyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplyException(Throwable cause) {
        super(cause);
    }

    protected ApplyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
