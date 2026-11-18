package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.CacheInvalidationTask;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.mapper.CacheInvalidationMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.utils.RedisUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("缓存补偿持久化重试单元测试")
@ExtendWith(MockitoExtension.class)
class CacheInvalidationWorkerTest {
    @Mock CacheInvalidationMapper mapper;
    @Mock KafkaProducer producer;
    @Mock PlatformTransactionManager transactionManager;
    @Mock RedisTemplate<String, Object> redis;
    @InjectMocks CacheInvalidationWorker worker;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", redis);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    @AfterEach void clear() { ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", null); }

    private CacheInvalidationTask task() {
        CacheInvalidationTask task = new CacheInvalidationTask();
        task.setId("test-id"); task.setPatterns("[\"activity:top\"]"); return task;
    }

    @Test @DisplayName("删除成功 - 独立事务清理任务且不发送重试")
    void testProcess_Success_Complete() {
        // Given - Redis 可以访问，键不存在也视为成功。
        CacheInvalidationTask task = task();
        // When - 尝试删除。
        worker.tryProcess(task);
        // Then - 任务完成发生在独立事务中。
        verify(redis).delete("activity:top");
        verify(mapper).complete("test-id");
        verify(transactionManager).getTransaction(argThat(t -> t.getPropagationBehavior()
                == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verifyNoInteractions(producer);
    }

    @Test @DisplayName("Redis和Kafka均不可用 - 保存任务等待定时重试")
    void testProcess_Failure_RemainsDurable() {
        // Given - Redis 删除异常，Kafka 默认返回发送失败。
        when(redis.delete("activity:top")).thenThrow(new RuntimeException("redis down"));
        // When - 提交后的补偿不应让已成功业务返回失败。
        assertDoesNotThrow(() -> worker.tryProcess(task()));
        // Then - 延迟重试且绝不完成任务。
        verify(mapper).defer("test-id");
        verify(mapper, never()).complete(anyString());
        verify(producer).send(eq(MqTopics.CACHE_RETRY), contains("test-id"));
    }

    @Test @DisplayName("应用恢复 - 从数据库加载遗留任务并完成删除")
    void testRetryPending_Restart_Recovers() {
        // Given - 模拟重启后仅数据库中存在待处理任务。
        when(mapper.findDue()).thenReturn(java.util.Collections.singletonList(task()));
        // When - 定时扫描。
        worker.retryPending();
        // Then - 不依赖内存队列即可恢复。
        verify(mapper).complete("test-id");
    }

    @Test @DisplayName("MQ重试失败 - 不立即重新投递形成消息风暴")
    void testRetry_ConsumerFailure_NoRepublish() {
        // Given - 队列消息对应任务存在，但 Redis 尚未恢复。
        when(mapper.find("test-id")).thenReturn(task());
        when(redis.delete("activity:top")).thenThrow(new RuntimeException("down"));
        // When - 消费重试消息。
        worker.retry("test-id");
        // Then - 任务延迟，后续由定时器恢复。
        verify(mapper).defer("test-id");
        verifyNoInteractions(producer);
    }
}
