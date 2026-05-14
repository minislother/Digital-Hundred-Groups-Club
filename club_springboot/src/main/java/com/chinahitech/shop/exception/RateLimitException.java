package com.chinahitech.shop.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}