package com.chinahitech.shop.aop;

import com.chinahitech.shop.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

@Aspect
@Component
public class RateLimiterAspect {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterAspect.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT;

    static {
        SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>();
        SLIDING_WINDOW_SCRIPT.setLocation(new ClassPathResource("limiter.lua"));
        SLIDING_WINDOW_SCRIPT.setResultType(Long.class);
    }

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        checkLimit(point, rateLimiter.key(), rateLimiter.window(), rateLimiter.limit(),
                rateLimiter.type(), rateLimiter.message());
    }

    @Before("@annotation(repeatLimit)")
    public void doBeforeRepeat(JoinPoint point, RepeatLimit repeatLimit) {
        checkLimit(point, repeatLimit.key(), repeatLimit.window(), repeatLimit.limit(),
                repeatLimit.type(), repeatLimit.message());
    }

    private void checkLimit(JoinPoint point, String key, long window, long limit,
                            RateLimiter.LimitType type, String message) {
        String fullKey = buildRateLimitKey(point, type, key);
        Long result = executeSlidingWindowScript(fullKey, window, limit);
        if (result != null && result == 0) {
            throw new RateLimitException(message);
        }
    }

    public Long executeSlidingWindowScript(String key, Long window, Long limit) {
        long now = System.currentTimeMillis();
        try {
            return stringRedisTemplate.execute(
                    SLIDING_WINDOW_SCRIPT,
                    Collections.singletonList(key),
                    window.toString(), limit.toString(), Long.toString(now)
            );
        } catch (Exception e) {
            LOG.warn("Redis rate limit failed, request is rejected. key={}", key, e);
            return 0L;
        }
    }

    private String buildRateLimitKey(JoinPoint point, RateLimiter.LimitType type, String baseKey) {
        StringBuilder keyBuilder = new StringBuilder(baseKey);

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        keyBuilder.append(method.getDeclaringClass().getName())
                .append(":")
                .append(method.getName());

        switch (type) {
            case IP:
                keyBuilder.append(":ip:").append(getClientIp());
                break;
            case USER:
                keyBuilder.append(":user:").append(getCurrentUserId());
                break;
            case METHOD:
            default:
                break;
        }

        return keyBuilder.toString();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip) ? request.getRemoteAddr() : ip;
    }

    private String getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "anonymous";
        }
        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.trim().isEmpty()) {
            userId = request.getParameter("userId");
        }
        return userId == null || userId.trim().isEmpty() ? "anonymous" : userId.trim();
    }
}
