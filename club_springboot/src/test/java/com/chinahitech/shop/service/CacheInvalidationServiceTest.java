package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.CacheInvalidationTask;
import com.chinahitech.shop.mapper.CacheInvalidationMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("缓存失效事务时序单元测试")
@ExtendWith(MockitoExtension.class)
class CacheInvalidationServiceTest {
    @Mock CacheInvalidationMapper mapper;
    @Mock CacheInvalidationWorker worker;
    @InjectMocks CacheInvalidationService service;
    @AfterEach void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test @DisplayName("缓存删除 - 提交前只保存任务提交后才删除")
    void testEvict_AfterCommitOnly() {
        // Given - 活跃事务。
        TransactionSynchronizationManager.initSynchronization();
        // When - 业务代码请求缓存失效。
        service.evict("activity:detail:1");
        // Then - 先保存恢复凭据，提交前不能访问缓存。
        ArgumentCaptor<CacheInvalidationTask> task = ArgumentCaptor.forClass(CacheInvalidationTask.class);
        verify(mapper).insert(task.capture());
        assertNotNull(task.getValue().getId());
        assertEquals("[\"activity:detail:1\"]", task.getValue().getPatterns());
        verifyNoInteractions(worker);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(worker).tryProcess(task.getValue());
    }

    @Test @DisplayName("缓存删除 - 事务回滚不执行删除")
    void testEvict_Rollback_NoDelete() {
        // Given - 事务中的删除请求。
        TransactionSynchronizationManager.initSynchronization();
        service.evict("activity:top");
        // When - 回滚事务。
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
        // Then - 不删除缓存。
        verifyNoInteractions(worker);
    }

    @Test @DisplayName("缓存删除 - 空范围拒绝保存任务")
    void testEvict_Empty_Reject() {
        // Given - 空删除范围。
        // When & Then - 不产生无意义任务。
        assertThrows(IllegalArgumentException.class, () -> service.evict());
        assertThrows(IllegalArgumentException.class, () -> service.evict((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> service.evict(" "));
        verifyNoInteractions(mapper, worker);
    }
}
