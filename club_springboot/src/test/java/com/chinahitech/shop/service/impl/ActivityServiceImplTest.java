package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("活动服务单元测试")
@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Test
    @DisplayName("活动报名 - 活动不存在抛出异常")
    void testApplyJoin_ActivityNotExist_ThrowException() {
        // Given - 准备测试数据和Mock行为
        when(activityMapper.getActivityById(1)).thenReturn(null);

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.applyJoin(1, 1001)
        );
        assertEquals("ACTIVITY_NOT_EXIST", exception.getCode());
        verify(activityMapper, never()).applyJoin(any(), any(), any());
    }

    @Test
    @DisplayName("活动报名 - 重复报名抛出异常")
    void testApplyJoin_RepeatApply_ThrowException() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(Collections.singletonMap("id", 1));

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.applyJoin(1, 1001)
        );
        assertEquals("APPLY_REPEAT", exception.getCode());
        verify(activityMapper, never()).applyJoin(any(), any(), any());
    }

    @Test
    @DisplayName("活动报名 - 人数已满抛出异常")
    void testApplyJoin_ActivityFull_ThrowException() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(2);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(2);

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.applyJoin(1, 1001)
        );
        assertEquals("ACTIVITY_FULL", exception.getCode());
        verify(activityMapper, never()).applyJoin(any(), any(), any());
    }

    @Test
    @DisplayName("活动报名 - 成功写入待审核记录")
    void testApplyJoin_Success_InsertPendingRecord() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(1);
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(0L);

        // When - 执行被测试方法
        activityService.applyJoin(1, 1001);

        // Then - 验证结果和交互
        verify(activityMapper).applyJoin(1, 1001, 0);
    }

    @Test
    @DisplayName("活动报名 - Redis判定重复报名抛出异常")
    void testApplyJoin_GuardRepeat_ThrowException() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(1);
        whenExecuteGuardReturn(1L);

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.applyJoin(1, 1001)
        );
        assertEquals("APPLY_REPEAT", exception.getCode());
        verify(activityMapper, never()).applyJoin(any(), any(), any());
    }

    @Test
    @DisplayName("活动报名 - Redis判定人数已满抛出异常")
    void testApplyJoin_GuardFull_ThrowException() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(8);
        whenExecuteGuardReturn(2L);

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.applyJoin(1, 1001)
        );
        assertEquals("ACTIVITY_FULL", exception.getCode());
        verify(activityMapper, never()).applyJoin(any(), any(), any());
    }

    @Test
    @DisplayName("活动报名 - Redis异常时降级写入数据库")
    void testApplyJoin_GuardException_InsertPendingRecord() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(1);
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new RuntimeException("redis down"));

        // When - 执行被测试方法
        activityService.applyJoin(1, 1001);

        // Then - 验证结果和交互
        verify(activityMapper).applyJoin(1, 1001, 0);
    }

    @Test
    @DisplayName("活动报名 - 数据库插入失败回滚Redis报名标记")
    void testApplyJoin_InsertFailed_RollbackGuard() {
        // Given - 准备测试数据和Mock行为
        Activity activity = createActivity(10);
        when(activityMapper.getActivityById(1)).thenReturn(activity);
        when(activityMapper.checkApply(1, 1001)).thenReturn(null);
        when(activityMapper.countActiveApplications(1)).thenReturn(1);
        whenExecuteGuardReturn(0L);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new RuntimeException("insert failed"))
                .when(activityMapper).applyJoin(1, 1001, 0);

        // When & Then - 执行被测试方法并验证异常
        assertThrows(RuntimeException.class, () -> activityService.applyJoin(1, 1001));
        verify(setOperations).remove("activity:apply:students:1", "1001");
        verify(valueOperations).decrement("activity:apply:count:1");
    }

    private void whenExecuteGuardReturn(Long value) {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(value);
    }

    private Activity createActivity(int number) {
        Activity activity = new Activity();
        activity.setId(1);
        activity.setName("迎新活动");
        activity.setGroupName("计算机协会");
        activity.setNumber(number);
        return activity;
    }
}
