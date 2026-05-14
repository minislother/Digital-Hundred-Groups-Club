package com.chinahitech.shop.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatLimit {
    String key() default "repeat_limit:";

    int window() default 10;

    int limit() default 20;

    String message() default "请求过于频繁，请稍后再试";

    RateLimiter.LimitType type() default RateLimiter.LimitType.IP;
}
