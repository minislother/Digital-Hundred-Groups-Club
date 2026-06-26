package com.chinahitech.shop.exception;

/**
 * 请求频率超过限流规则时抛出的异常。
 */
public class RateLimitException extends RuntimeException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public RateLimitException(String message) {
        super(message);
    }
}
