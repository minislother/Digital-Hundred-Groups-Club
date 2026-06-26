package com.chinahitech.shop.exception;

/**
 * Redis 写入或验证码缓存保存失败时抛出的业务异常。
 */
public class RedisAddException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public RedisAddException() {
        super();
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    public RedisAddException(String message) {
        super(message);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    public RedisAddException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    public RedisAddException(Throwable cause) {
        super(cause);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    protected RedisAddException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
