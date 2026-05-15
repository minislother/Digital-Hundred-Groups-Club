package com.chinahitech.shop.exception;

/**
 * Redis 写入或验证码缓存保存失败时抛出的业务异常。
 */
public class RedisAddException extends ServiceException {
    public RedisAddException() {
        super();
    }

    public RedisAddException(String message) {
        super(message);
    }

    public RedisAddException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisAddException(Throwable cause) {
        super(cause);
    }

    protected RedisAddException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
