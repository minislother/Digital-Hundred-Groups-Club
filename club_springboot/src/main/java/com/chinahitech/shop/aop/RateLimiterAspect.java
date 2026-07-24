package com.chinahitech.shop.aop;

import com.chinahitech.shop.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * 本地开发环境可能没有启动 Redis。为避免登录接口因为限流组件不可用而失败，默认采用放行策略；
     * 生产环境如果要求 Redis 异常时直接拒绝请求，可设置 RATE_LIMIT_FAIL_OPEN=false。
     */
    @Value("${app.rate-limit.fail-open:true}")
    private boolean failOpen;

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

    /**
     * 执行 Redis 滑动窗口脚本并返回限流结果。
     */
    public Long executeSlidingWindowScript(String key, Long window, Long limit) {
        long now = System.currentTimeMillis();
        try {
            return stringRedisTemplate.execute(
                    SLIDING_WINDOW_SCRIPT,
                    Collections.singletonList(key),
                    window.toString(), limit.toString(), Long.toString(now)
            );
        } catch (Exception e) {
            if (failOpen) {
                // Redis 未启动时只记录摘要，避免登录接口每次请求都输出整段堆栈。
                LOG.warn("Redis rate limit unavailable, request is allowed. key={}, cause={}",
                        key, e.getMessage());
                return 1L;
            }
            throw e;
        }
    }

    /**
     * 根据限流类型生成 Redis 限流 key。
     */
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

    /**
     * 获取当前请求的客户端 IP 地址。
     */
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

    /**
     * 获取当前登录用户 ID，用于用户维度限流。
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().trim().isEmpty()) {
            return "anonymous";
        }
        return authentication.getName().trim();
    }
}
