package com.chinahitech.shop.exception;

/**
 * 请求频率超过限流规则时抛出的异常。
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
