package com.chinahitech.shop.utils;

import lombok.extern.slf4j.Slf4j;
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

    @Resource
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
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
