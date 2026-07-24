package com.chinahitech.shop.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证业务缓存开关只控制查询缓存访问，不改变 RedisUtils 的基础能力。
 */
@DisplayName("RedisUtils business cache switch tests")
@ExtendWith(MockitoExtension.class)
class RedisUtilsTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisUtils redisUtils;

    @BeforeEach
    void setUp() {
        redisUtils = new RedisUtils();
        redisUtils.setRedisTemplate(redisTemplate);
        redisUtils.setBusinessCacheEnabled(true);
    }

    @AfterEach
    void tearDown() {
        // 静态开关恢复默认值，避免影响同一 JVM 中的其它单元测试。
        redisUtils.setBusinessCacheEnabled(true);
        redisUtils.setRedisTemplate(null);
    }

    @Test
    @DisplayName("disabled cache bypasses Redis reads and writes")
    void businessCacheDisabled_BypassRedis() {
        redisUtils.setBusinessCacheEnabled(false);

        assertNull(RedisUtils.getBusinessCache("group:all:"));
        assertTrue(RedisUtils.setBusinessCache("group:all:", "value", 60));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("enabled cache delegates reads and writes to Redis")
    void businessCacheEnabled_DelegateToRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("group:all:")).thenReturn("cached-value");

        assertEquals("cached-value", RedisUtils.getBusinessCache("group:all:"));
        assertTrue(RedisUtils.setBusinessCache("group:all:", "new-value", 60));
        verify(valueOperations).set("group:all:", "new-value", 60,
                java.util.concurrent.TimeUnit.SECONDS);
    }
}
