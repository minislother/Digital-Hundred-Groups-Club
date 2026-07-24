package com.chinahitech.shop.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtils {

    public static final String NULL_VALUE = "__NULL__";

    private static RedisTemplate<String, Object> redisTemplate;
    private static volatile boolean businessCacheEnabled = true;

    /**
     * 更新对象属性或页面状态值。
     */
    @Resource
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
    }

    /**
     * 配置业务查询缓存是否启用。该开关只影响查询结果缓存，不影响限流、验证码、
     * 报名防重复和分布式锁等必须依赖 Redis 的功能。
     */
    @Value("${app.business-cache.enabled:true}")
    public void setBusinessCacheEnabled(boolean enabled) {
        RedisUtils.businessCacheEnabled = enabled;
    }

    /**
     * 读取业务查询缓存；关闭缓存时直接返回未命中，让 Service 回源数据库。
     */
    public static Object getBusinessCache(String key) {
        return businessCacheEnabled ? get(key) : null;
    }

    /**
     * 写入永久业务查询缓存；关闭缓存时执行成功的空操作，避免影响主业务流程。
     */
    public static boolean setBusinessCache(String key, Object value) {
        return !businessCacheEnabled || set(key, value);
    }

    /**
     * 写入带过期时间的业务查询缓存；关闭缓存时不访问 Redis。
     */
    public static boolean setBusinessCache(String key, Object value, long time) {
        return !businessCacheEnabled || set(key, value, time);
    }

    public static boolean del(String key) {
        if (key == null) {
            log.warn("[Redis delete] key is null");
            return false;
        }
        if (redisTemplate == null) {
            log.warn("[Redis delete] RedisTemplate is not initialized. key={}", key);
            return false;
        }
        try {
            if (key.contains("*")) {
                Set<String> keys = scanKeys(key);
                if (CollectionUtils.isEmpty(keys)) {
                    return true;
                }
                redisTemplate.delete(keys);
            } else {
                redisTemplate.delete(key);
            }
            log.info("[Redis delete] success. key={}", key);
            return true;
        } catch (Exception e) {
            log.error("[Redis delete] failed. key={}", key, e);
            return false;
        }
    }

    private static Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisConnection connection) -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Scan redis keys failed", e);
            }
            return keys;
        });
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public static Object get(String key) {
        if (key == null) {
            log.warn("[Redis get] key is null");
            return null;
        }
        if (redisTemplate == null) {
            log.warn("[Redis get] RedisTemplate is not initialized. key={}", key);
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("[Redis get] failed. key={}", key, e);
            return null;
        }
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public static boolean set(String key, Object value) {
        if (key == null) {
            log.warn("[Redis set] key is null");
            return false;
        }
        if (redisTemplate == null) {
            log.warn("[Redis set] RedisTemplate is not initialized. key={}", key);
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("[Redis set] failed. key={}", key, e);
            return false;
        }
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public static boolean set(String key, Object value, long time) {
        if (time <= 0) {
            return set(key, value);
        }
        if (key == null) {
            log.warn("[Redis set] key is null");
            return false;
        }
        if (redisTemplate == null) {
            log.warn("[Redis set] RedisTemplate is not initialized. key={}", key);
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("[Redis set] failed. key={}", key, e);
            return false;
        }
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public static boolean setIfAbsent(String key, Object value, long time) {
        if (key == null) {
            log.warn("[Redis setIfAbsent] key is null");
            return false;
        }
        if (redisTemplate == null) {
            log.warn("[Redis setIfAbsent] RedisTemplate is not initialized. key={}", key);
            return false;
        }
        try {
            Boolean result;
            if (time > 0) {
                result = redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
            } else {
                result = redisTemplate.opsForValue().setIfAbsent(key, value);
            }
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[Redis setIfAbsent] failed. key={}", key, e);
            return false;
        }
    }
}
