package com.chinahitech.shop.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证业务缓存开关、L1/L2 读写顺序，以及多实例之间的本地缓存失效行为。
 */
@DisplayName("RedisUtils 多级业务缓存单元测试")
@ExtendWith(MockitoExtension.class)
class RedisUtilsTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private Message message;

    private RedisUtils redisUtils;

    @BeforeEach
    void setUp() {
        redisUtils = new RedisUtils();
        redisUtils.setRedisTemplate(redisTemplate);
        redisUtils.setStringRedisTemplate(stringRedisTemplate);
        redisUtils.setBusinessCacheEnabled(true);
        redisUtils.configureLocalBusinessCache(true, 100, 60);
    }

    @AfterEach
    void tearDown() {
        // 静态缓存和开关必须复位，避免同一 JVM 中的其它测试互相污染。
        redisUtils.setBusinessCacheEnabled(true);
        redisUtils.configureLocalBusinessCache(false, 100, 60);
        redisUtils.setStringRedisTemplate(null);
        redisUtils.setRedisTemplate(null);
    }

    @Test
    @DisplayName("关闭业务缓存 - 跳过 L1 和 Redis 读写")
    void testBusinessCacheDisabled_BypassRedis() {
        // Given - 关闭整个业务查询缓存。
        redisUtils.setBusinessCacheEnabled(false);

        // When - 读写业务缓存。
        assertNull(RedisUtils.getBusinessCache("group:all:"));
        assertTrue(RedisUtils.setBusinessCache("group:all:", "value", 60));

        // Then - 不应访问 Redis。
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("启用业务缓存 - 未命中时读写 Redis")
    void testBusinessCacheEnabled_DelegateToRedis() {
        // Given - Redis 中已有缓存值。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("group:all:")).thenReturn("cached-value");

        // When - 读取后更新业务缓存。
        assertEquals("cached-value", RedisUtils.getBusinessCache("group:all:"));
        assertTrue(RedisUtils.setBusinessCache("group:all:", "new-value", 60));

        // Then - 新值按指定 TTL 写入 Redis。
        verify(valueOperations).set("group:all:", "new-value", 60, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("L1 未命中 - 只读取一次 Redis 并回填本地缓存")
    void testLocalCacheMiss_LoadRedisAndWarmL1() {
        // Given - Redis 中存在社团详情缓存。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("group:detail:photography")).thenReturn("redis-value");

        // When - 连续读取同一个业务键。
        assertEquals("redis-value", RedisUtils.getBusinessCache("group:detail:photography"));
        assertEquals("redis-value", RedisUtils.getBusinessCache("group:detail:photography"));

        // Then - 第二次应直接命中 L1。
        verify(valueOperations, times(1)).get("group:detail:photography");
    }

    @Test
    @DisplayName("写入业务缓存 - 同时填充 Redis 和 L1")
    void testBusinessCacheWrite_PopulateBothLevels() {
        // Given - Redis 写操作可用。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When - 写入带 TTL 的活动详情。
        assertTrue(RedisUtils.setBusinessCache("activity:detail:1", "new-value", 60));
        clearInvocations(redisTemplate, valueOperations);

        // Then - 后续读取直接命中 L1。
        assertEquals("new-value", RedisUtils.getBusinessCache("activity:detail:1"));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("通配符失效 - 只清除匹配的 L1 缓存")
    void testLocalCachePatternEviction_RemoveMatchingEntries() {
        // Given - L1 中分别存在社团和活动缓存。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        assertTrue(RedisUtils.setBusinessCache("group:detail:1", "group-value", 60));
        assertTrue(RedisUtils.setBusinessCache("activity:detail:1", "activity-value", 60));
        clearInvocations(redisTemplate, valueOperations);
        when(valueOperations.get("group:detail:1")).thenReturn("group-from-redis");

        // When - 按社团前缀清理本地缓存。
        RedisUtils.evictLocalBusinessCache("group:*");

        // Then - 社团数据回源 Redis，活动数据仍命中 L1。
        assertEquals("group-from-redis", RedisUtils.getBusinessCache("group:detail:1"));
        assertEquals("activity-value", RedisUtils.getBusinessCache("activity:detail:1"));
        verify(valueOperations, times(1)).get("group:detail:1");
        verify(valueOperations, never()).get("activity:detail:1");
    }

    @Test
    @DisplayName("收到远程失效消息 - 清除当前实例的 L1 缓存")
    void testInvalidationMessage_EvictLocalEntry() {
        // Given - 当前实例缓存旧值，Redis 中准备返回新值。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        assertTrue(RedisUtils.setBusinessCache("activity:detail:2", "old-value", 60));
        clearInvocations(redisTemplate, valueOperations);
        when(message.getBody()).thenReturn("activity:detail:2".getBytes(StandardCharsets.UTF_8));
        when(valueOperations.get("activity:detail:2")).thenReturn("fresh-value");

        // When - 收到另一个实例发布的精确键失效消息。
        redisUtils.onMessage(message, null);

        // Then - 本地旧值被清除，读取回源 Redis。
        assertEquals("fresh-value", RedisUtils.getBusinessCache("activity:detail:2"));
        verify(valueOperations).get("activity:detail:2");
    }

    @Test
    @DisplayName("关闭 L1 缓存 - 每次查询都访问 Redis")
    void testLocalCacheDisabled_AlwaysReadRedis() {
        // Given - 业务缓存启用，但单独关闭进程内 L1。
        redisUtils.configureLocalBusinessCache(false, 100, 60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("group:detail:2")).thenReturn("redis-value");

        // When - 连续读取同一个键。
        assertEquals("redis-value", RedisUtils.getBusinessCache("group:detail:2"));
        assertEquals("redis-value", RedisUtils.getBusinessCache("group:detail:2"));

        // Then - 两次请求都直接访问 Redis。
        verify(valueOperations, times(2)).get("group:detail:2");
    }

    @Test
    @DisplayName("删除业务缓存 - 清理 L1、Redis 并广播失效")
    void testDeleteBusinessCache_EvictBothLevelsAndPublish() {
        // Given - 两级缓存中存在旧值。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        assertTrue(RedisUtils.setBusinessCache("activity:detail:3", "old-value", 60));
        clearInvocations(redisTemplate, valueOperations, stringRedisTemplate);
        when(valueOperations.get("activity:detail:3")).thenReturn("fresh-value");

        // When - 删除业务缓存键。
        assertTrue(RedisUtils.del("activity:detail:3"));

        // Then - Redis 被删除、失效消息被广播，下一次读取回源 L2。
        verify(redisTemplate).delete("activity:detail:3");
        verify(stringRedisTemplate).convertAndSend(
                RedisUtils.LOCAL_CACHE_INVALIDATION_CHANNEL, "activity:detail:3");
        assertEquals("fresh-value", RedisUtils.getBusinessCache("activity:detail:3"));
        verify(valueOperations).get("activity:detail:3");
    }
}
