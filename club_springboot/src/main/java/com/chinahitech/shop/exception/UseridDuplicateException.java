package com.chinahitech.shop.exception;

/**
 * 新增用户时账号已存在或批量导入账号重复时抛出的业务异常。
 */
public class UseridDuplicateException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public UseridDuplicateException() {
        super();
    }

    public UseridDuplicateException(String message) {
        super(message);
    }

    public UseridDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public UseridDuplicateException(Throwable cause) {
        super(cause);
    }

    protected UseridDuplicateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
