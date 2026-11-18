package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.exception.ActivityRegistrationConflict;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mapper.IndividualActivityMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 验证唯一键失败不会误报成功、发送后续事件或留下本次 Redis 预占。 */
@DisplayName("报名唯一约束与事务补偿单元测试")
@ExtendWith(MockitoExtension.class)
class ActivityAdmissionUniquenessTest {
    @Mock ActivityMapper activityMapper;
    @Mock IndividualActivityMapper individualActivityMapper;
    @Mock StringRedisTemplate redis;
    @Mock CacheInvalidationService invalidation;
    @Mock ActivityAdmissionOutboxService outboxService;
    @Mock ActivityAdmissionRecovery recovery;
    @InjectMocks ActivityAdmissionService service;

    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    private Activity activity() {
        Activity a=new Activity();a.setId(31);a.setNumber(10);return a;
    }
    private DuplicateKeyException duplicate() {
        return new DuplicateKeyException("Duplicate entry for key 'individual_activity."+ActivityRegistrationConflict.INDEX_NAME+"'");
    }
    private void reserve() {
        when(activityMapper.getActivityById(31)).thenReturn(activity());
        when(activityMapper.getActivityForUpdate(31)).thenReturn(activity());
        when(redis.execute(any(RedisScript.class),anyList(),eq("student"),eq("10"),eq("86400"),anyString())).thenReturn(1L);
    }

    @Test @DisplayName("同步报名 - 唯一键冲突返回重复报名且立即补偿预占")
    void testRegister_Duplicate_RejectAndRelease() {
        // Given
        reserve();doThrow(duplicate()).when(individualActivityMapper).insert(any());
        // When
        BusinessException error=assertThrows(BusinessException.class,()->service.register(31,"student","成员",0));
        // Then
        assertEquals("APPLY_REPEAT",error.getCode());
        verify(redis).execute(any(RedisScript.class),anyList(),eq("student"),anyString());
        verifyNoInteractions(invalidation,outboxService);
    }

    @Test @DisplayName("事务报名 - 唯一键冲突在回滚完成后补偿")
    void testRegister_TransactionRollback_ReleaseAfterCompletion() {
        // Given
        reserve();doThrow(duplicate()).when(individualActivityMapper).insert(any());
        TransactionSynchronizationManager.initSynchronization();
        // When
        assertThrows(BusinessException.class,()->service.register(31,"student","成员",0));
        // Then - 方法抛出时事务尚未完成，不能先释放名额。
        verify(redis,never()).execute(any(RedisScript.class),anyList(),eq("student"),anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(redis).execute(any(RedisScript.class),anyList(),eq("student"),anyString());
    }

    @Test @DisplayName("同步报名 - 其他唯一键异常不误报重复报名")
    void testRegister_OtherUniqueKey_PreserveError() {
        // Given
        reserve();DuplicateKeyException error=new DuplicateKeyException("Duplicate entry for key 'PRIMARY'");
        doThrow(error).when(individualActivityMapper).insert(any());
        // When & Then
        assertSame(error,assertThrows(DuplicateKeyException.class,()->service.register(31,"student","成员",0)));
        verifyNoInteractions(invalidation);
    }

    @Test @DisplayName("异步消费 - 查询后并发插入冲突按已存在记录幂等处理")
    void testConsumeAdmission_ConcurrentDuplicate_Idempotent() {
        // Given
        when(activityMapper.getActivityForUpdate(31)).thenReturn(activity());
        when(individualActivityMapper.checkExist("student",31)).thenReturn(null,new IndividualActivity());
        doThrow(duplicate()).when(individualActivityMapper).insert(any());
        // When
        service.consumeAdmission(31,"student","成员",0);
        // Then
        verifyNoInteractions(invalidation, redis);
    }

    @Test @DisplayName("异步消费 - 无法确认已落库时不能吞掉唯一键错误")
    void testConsumeAdmission_NoExistingRecord_Propagate() {
        // Given
        when(activityMapper.getActivityForUpdate(31)).thenReturn(activity());
        DuplicateKeyException error=duplicate();doThrow(error).when(individualActivityMapper).insert(any());
        // When & Then
        assertSame(error,assertThrows(DuplicateKeyException.class,()->service.consumeAdmission(31,"student","成员",0)));
        verifyNoInteractions(redis,invalidation);
    }

    @Test @DisplayName("Redis未初始化 - 不将返回码4当预占成功或错误提交异步任务")
    void testRegister_UninitializedCounter_UseDatabase() {
        // Given
        when(activityMapper.getActivityById(31)).thenReturn(activity());
        when(activityMapper.getActivityForUpdate(31)).thenReturn(activity());
        when(redis.execute(any(RedisScript.class),anyList(),eq("student"),eq("10"),eq("86400"),anyString())).thenReturn(4L);
        ReflectionTestUtils.setField(service,"asyncWriteEnabled",true);
        // When
        service.register(31,"student","成员",0);
        // Then
        verify(individualActivityMapper).insert(any());
        verifyNoInteractions(outboxService);
        verify(redis,never()).execute(any(RedisScript.class),anyList(),eq("student"),anyString());
    }
}
