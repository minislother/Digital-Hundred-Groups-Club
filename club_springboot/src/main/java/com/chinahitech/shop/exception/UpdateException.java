package com.chinahitech.shop.exception;

/**
 * 修改数据库记录失败时抛出的业务异常。
 */
public class UpdateException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public UpdateException() {
        super();
    }

    /**
     * 更新表单字段或业务数据。
     */
    public UpdateException(String message) {
        super(message);
    }

    /**
     * 更新表单字段或业务数据。
     */
    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 更新表单字段或业务数据。
     */
    public UpdateException(Throwable cause) {
        super(cause);
    }

    /**
     * 更新表单字段或业务数据。
     */
    protected UpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
