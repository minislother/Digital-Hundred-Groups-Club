package com.chinahitech.shop.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtils implements MessageListener {

    public static final String NULL_VALUE = "__NULL__";
    public static final String LOCAL_CACHE_INVALIDATION_CHANNEL = "baituan:business-cache:invalidate";

    private static RedisTemplate<String, Object> redisTemplate;
    private static StringRedisTemplate stringRedisTemplate;
    private static volatile boolean businessCacheEnabled = true;
    // 默认关闭，待 Spring 注入配置后启用；这样不启动容器的纯单元测试不会共享静态 L1 数据。
    private static volatile boolean localBusinessCacheEnabled = false;
    private static volatile Cache<String, Object> localBusinessCache = newLocalBusinessCache(10_000, 30);

    /**
     * 更新对象属性或页面状态值。
     */
    @Resource
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        localBusinessCache.invalidateAll();
        RedisUtils.redisTemplate = redisTemplate;
    }

    @Resource
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        RedisUtils.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 初始化进程内 L1 缓存。maximumSize 防止热点数据无限占用堆内存，TTL 则限制多实例场景下
     * 因失效消息暂时不可用而产生的最大陈旧窗口。
     */
    @Autowired
    public void configureLocalBusinessCache(
            @Value("${app.business-cache.local.enabled:true}") boolean enabled,
            @Value("${app.business-cache.local.maximum-size:10000}") long maximumSize,
            @Value("${app.business-cache.local.ttl-seconds:30}") long ttlSeconds) {
        localBusinessCacheEnabled = enabled;
        localBusinessCache = newLocalBusinessCache(maximumSize, ttlSeconds);
    }

    /**
     * 配置业务查询缓存是否启用。该开关只影响查询结果缓存，不影响限流、验证码、
     * 报名防重复和分布式锁等必须依赖 Redis 的功能。
     */
    @Value("${app.business-cache.enabled:true}")
    public void setBusinessCacheEnabled(boolean enabled) {
        RedisUtils.businessCacheEnabled = enabled;
        if (!enabled) {
            localBusinessCache.invalidateAll();
        }
    }

    /**
     * 读取业务查询缓存；关闭缓存时直接返回未命中，让 Service 回源数据库。
     */
    public static Object getBusinessCache(String key) {
        if (!businessCacheEnabled || key == null) {
            return null;
        }
        if (localBusinessCacheEnabled) {
            Object localValue = localBusinessCache.getIfPresent(key);
            if (localValue != null) {
                return localValue;
            }
        }

        Object redisValue = get(key);
        if (redisValue != null && localBusinessCacheEnabled) {
            localBusinessCache.put(key, redisValue);
        }
        return redisValue;
    }

    /**
     * 写入永久业务查询缓存；关闭缓存时执行成功的空操作，避免影响主业务流程。
     */
    public static boolean setBusinessCache(String key, Object value) {
        if (!businessCacheEnabled) {
            evictLocalBusinessCache(key);
            return true;
        }
        boolean stored = set(key, value);
        if (stored && localBusinessCacheEnabled && key != null && value != null) {
            localBusinessCache.put(key, value);
        }
        return stored;
    }

    /**
     * 写入带过期时间的业务查询缓存；关闭缓存时不访问 Redis。
     */
    public static boolean setBusinessCache(String key, Object value, long time) {
        if (!businessCacheEnabled) {
            evictLocalBusinessCache(key);
            return true;
        }
        boolean stored = set(key, value, time);
        if (stored && localBusinessCacheEnabled && key != null && value != null) {
            localBusinessCache.put(key, value);
        }
        return stored;
    }

    public static boolean del(String key) {
        if (key == null) {
            log.warn("[Redis delete] key is null");
            return false;
        }
        // 先清本实例 L1，再删除共享 L2；即使 Redis 暂时不可用，也不会继续返回本地旧数据。
        evictLocalBusinessCache(key);
        if (redisTemplate == null) {
            log.warn("[Redis delete] RedisTemplate is not initialized. key={}", key);
            return false;
        }
        try {
            if (key.contains("*")) {
                Set<String> keys = scanKeys(key);
                if (!CollectionUtils.isEmpty(keys)) {
                    redisTemplate.delete(keys);
                }
            } else {
                redisTemplate.delete(key);
            }
            publishLocalCacheInvalidation(key);
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
     * 接收其他应用实例发布的失效消息。这里只清 L1，不再次删除 Redis 或发布消息，避免消息循环。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null) {
            return;
        }
        evictLocalBusinessCache(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * 清除单个本地缓存键，或按 Redis 风格的通配模式批量清除本地键。
     */
    public static void evictLocalBusinessCache(String keyOrPattern) {
        if (keyOrPattern == null) {
            return;
        }
        if (!keyOrPattern.contains("*")) {
            localBusinessCache.invalidate(keyOrPattern);
            return;
        }
        for (String localKey : localBusinessCache.asMap().keySet()) {
            if (PatternMatchUtils.simpleMatch(keyOrPattern, localKey)) {
                localBusinessCache.invalidate(localKey);
            }
        }
    }

    private static Cache<String, Object> newLocalBusinessCache(long maximumSize, long ttlSeconds) {
        return Caffeine.newBuilder()
                .maximumSize(Math.max(1, maximumSize))
                .expireAfterWrite(Math.max(1, ttlSeconds), TimeUnit.SECONDS)
                .build();
    }

    private static void publishLocalCacheInvalidation(String keyOrPattern) {
        if (!localBusinessCacheEnabled || stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.convertAndSend(LOCAL_CACHE_INVALIDATION_CHANNEL, keyOrPattern);
        } catch (Exception e) {
            // Redis 删除已经完成时不回滚主流程；L1 的短 TTL 会作为最终一致性兜底。
            log.warn("[Local cache invalidation] publish failed. key={}", keyOrPattern, e);
        }
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
