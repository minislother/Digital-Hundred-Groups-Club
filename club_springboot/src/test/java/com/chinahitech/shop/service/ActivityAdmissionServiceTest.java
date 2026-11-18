package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mapper.IndividualActivityMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("活动名额统一服务单元测试")
@ExtendWith(MockitoExtension.class)
class ActivityAdmissionServiceTest {
    @Test @DisplayName("Redis故障限流 - 被拒绝请求不延长恢复窗口")
    void testRegister_FallbackRejected_RequestDoesNotPostponeRecovery() {
        // Given - 固定时钟避免测试依赖真实等待。
        Activity unlimited = new Activity(); unlimited.setId(1); unlimited.setNumber(0);
        when(activityMapper.getActivityById(1)).thenReturn(unlimited);
        when(activityMapper.getActivityForUpdate(1)).thenReturn(unlimited);
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis offline"));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fallbackMaxRps", 1L);
        setTime(10000);
        service.register(1, "s1", "成员", 0);
        // When - 半个窗口后的请求应被拒绝。
        setTime(10500);
        assertEquals("SYSTEM_BUSY", assertThrows(BusinessException.class,
                () -> service.register(1, "s2", "成员", 0)).getCode());
        // Then - 从上次放行起满一个窗口即可恢复，不受拒绝请求影响。
        setTime(11001);
        service.register(1, "s3", "成员", 0);
        verify(individualActivityMapper, times(2)).insert(any());
    }

    private void setTime(long millis) {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fallbackClock",
                java.time.Clock.fixed(java.time.Instant.ofEpochMilli(millis), java.time.ZoneOffset.UTC));
    }
    @Mock ActivityMapper activityMapper;
    @Mock IndividualActivityMapper individualActivityMapper;
    @Mock StringRedisTemplate redis;
    @Mock CacheInvalidationService invalidation;
    @Mock ActivityAdmissionRecovery recovery;
    @InjectMocks ActivityAdmissionService service;

    @AfterEach void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void activity(int limit, int count) {
        Activity activity = new Activity();
        activity.setId(1); activity.setNumber(limit);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.getActivityForUpdate(1)).thenReturn(activity);
        when(activityMapper.countActiveApplications(1)).thenReturn(count);
        when(redis.execute(any(RedisScript.class), anyList(), eq("s1"), eq(String.valueOf(limit)), eq("86400"), anyString())).thenReturn(1L);
    }

    @Test @DisplayName("报名 - Lua预占后写入并持久化缓存失效任务")
    void testRegister_Success_OrderAndState() {
        // Given - 还剩一个名额。
        activity(2, 1);
        // When - 学生申请。
        service.register(1, "s1", "成员", 0);
        // Then - 操作顺序和状态均正确，缓存删除不匹配占位命名空间。
        InOrder order = inOrder(activityMapper, individualActivityMapper, redis, invalidation);

        order.verify(redis).execute(any(RedisScript.class), eq(ActivityAdmissionService.reservationKeys(1)),
                eq("s1"), eq("2"), eq("86400"), anyString());
        order.verify(individualActivityMapper).insert(argThat(a -> a.getActivityId() == 1
                && a.getStatus() == 0 && "s1".equals(a.getStudentId())));
        order.verify(invalidation).evict("activity:all:*", "activity:page:*", "activity:detail:*",
                "activity:top", "activity:managed:*", "individualActivity:*");
    }

    @Test @DisplayName("报名 - Lua判定满额后不插入记录")
    void testRegister_Full_Reject() {
        // Given - 人数达到上限。
        Activity full = new Activity(); full.setId(1); full.setNumber(1);
        when(activityMapper.getActivityById(1)).thenReturn(full);
        when(redis.execute(any(RedisScript.class), anyList(), eq("s1"), eq("1"), eq("86400"), anyString())).thenReturn(2L);
        // When & Then - 数据库校验拒绝报名。
        assertEquals("ACTIVITY_FULL", assertThrows(BusinessException.class,
                () -> service.register(1, "s1", "成员", 0)).getCode());
        verifyNoInteractions(invalidation);
        verify(individualActivityMapper, never()).insert(any());
    }

    @Test @DisplayName("报名 - 重复申请被拒绝")
    void testRegister_Duplicate_Reject() {
        // Given - 已存在申请。
        Activity activity = new Activity(); activity.setId(1);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(redis.execute(any(RedisScript.class), anyList(), eq("s1"), eq("0"), eq("86400"), anyString())).thenReturn(3L);
        // When & Then - 不重复占位。
        assertEquals("APPLY_REPEAT", assertThrows(BusinessException.class,
                () -> service.register(1, "s1", "成员", 0)).getCode());
        verifyNoInteractions(invalidation);
    }

    @Test @DisplayName("报名 - 活动不存在时拒绝")
    void testRegister_MissingActivity_Reject() {
        // Given - 查询不到活动。
        // When & Then - 不执行任何预占或写入。
        assertEquals("ACTIVITY_NOT_EXIST", assertThrows(BusinessException.class,
                () -> service.register(1, "s1", "成员", 0)).getCode());
        verifyNoInteractions(redis, individualActivityMapper);
    }

    @Test @DisplayName("报名 - Redis异常仍在数据库锁保护下写入")
    void testRegister_RedisUnavailable_DatabaseFallback() {
        // Given - Redis 超时，数据库还有名额。
        Activity unlimited = new Activity(); unlimited.setId(1); unlimited.setNumber(0);
        when(activityMapper.getActivityById(1)).thenReturn(unlimited);
        when(activityMapper.getActivityForUpdate(1)).thenReturn(unlimited);
        when(activityMapper.countActiveApplications(1)).thenReturn(100);
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("timeout"));
        // When - 不限人数的活动允许报名。
        service.register(1, "s1", "成员", 0);
        // Then - 已获得活动行锁，数据库记录正常写入。
        verify(activityMapper).getActivityForUpdate(1);
        verify(individualActivityMapper).insert(any());
    }

    @Test @DisplayName("报名 - Lua 返回未知状态时不走数据库降级")
    void testRegister_UnexpectedRedisResult_RejectsWithoutFallback() {
        // Given
        Activity activity = new Activity();
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(redis.execute(any(RedisScript.class), anyList(), eq("s1"), eq("0"), eq("86400"), anyString()))
                .thenReturn(99L);

        // When
        assertThrows(IllegalStateException.class, () -> service.register(1, "s1", "成员", 0));

        // Then
        verify(activityMapper, never()).getActivityForUpdate(1);
        verifyNoInteractions(individualActivityMapper);
    }

    @Test @DisplayName("报名 - 事务结果未知时保留 Redis 预占")
    void testRegister_UnknownCompletion_DoesNotRelease() {
        // Given
        activity(2, 0);
        TransactionSynchronizationManager.initSynchronization();
        service.register(1, "s1", "成员", 0);

        // When
        TransactionSynchronizationManager.getSynchronizations().forEach(
                s -> s.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN));

        // Then
        verify(redis, never()).execute(any(RedisScript.class), anyList(), eq("s1"), anyString());
    }

    @Test @DisplayName("报名 - 事务提交阶段回滚也会释放相同请求的预占")
    void testRegister_TransactionRollback_ReleasesToken() {
        // Given - 开启事务同步，模拟方法返回后事务回滚。
        activity(2, 0);
        TransactionSynchronizationManager.initSynchronization();
        service.register(1, "s1", "成员", 0);
        var token = ArgumentCaptor.forClass(String.class);
        verify(redis).execute(any(RedisScript.class), anyList(), eq("s1"), eq("2"), eq("86400"), token.capture());
        // When - 外层事务失败。
        TransactionSynchronizationManager.getSynchronizations().forEach(
                s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        // Then - 使用同一个 token 原子释放，避免误减其他请求。
        verify(redis).execute(any(RedisScript.class), anyList(), eq("s1"), eq(token.getValue()));
    }

    @Test @DisplayName("报名 - 提交成功由持久化任务释放预占")
    void testRegister_Commit_DoesNotRelease() {
        // Given - 成功事务。
        activity(2, 0);
        TransactionSynchronizationManager.initSynchronization();
        service.register(1, "s1", "成员", 0);
        // When - 数据库提交。
        TransactionSynchronizationManager.getSynchronizations().forEach(
                s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        // Then - 由已提交的数据库任务释放，提交回调不直接调用 Redis。
        verify(recovery).recordSynchronousSuccess(eq(1), eq("s1"), anyString());
        verify(redis, never()).execute(any(RedisScript.class), anyList(), anyString(), anyString());
    }

    @Test @DisplayName("恢复已拒绝申请 - 满额时禁止重新占用名额")
    void testChangeStatus_RejectedAndFull_Reject() {
        // Given - 被拒绝申请不计入人数，但恢复时必须再次检查上限。
        Activity full = new Activity(); full.setId(1); full.setNumber(1);
        when(activityMapper.getActivityForUpdate(1)).thenReturn(full);
        when(activityMapper.countActiveApplications(1)).thenReturn(1);
        IndividualActivity existing = new IndividualActivity(); existing.setStatus(-1);
        when(individualActivityMapper.checkExist("s1", 1)).thenReturn(existing);
        // When & Then - 不能通过审核绕过人数限制。
        assertThrows(BusinessException.class, () -> service.changeStatus(1, "s1", 1));
        verify(individualActivityMapper, never()).updateStatus(anyInt(), anyString(), anyInt());
    }

    @Test @DisplayName("报名 - 写库失败且Redis补偿失败时保留原始异常")
    void testRegister_InsertAndReleaseFail_PreserveFailure() {
        // Given - 两个存储都出现故障。
        activity(2, 0);
        RuntimeException failure = new RuntimeException("database failed");
        doThrow(failure).when(individualActivityMapper).insert(any());
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenThrow(new RuntimeException("redis failed"));
        // When & Then - 下一次持锁操作会校准计数，此次不能掩盖真正的写库异常。
        assertSame(failure, assertThrows(RuntimeException.class, () -> service.register(1, "s1", "成员", 0)));
        verifyNoInteractions(invalidation);
        verify(recovery).recordRollback(eq(1), eq("s1"), anyString());
    }
}
