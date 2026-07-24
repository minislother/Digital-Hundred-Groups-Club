package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.utils.RedisUtils;
import com.chinahitech.shop.utils.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", null);
    }

    @Test
    @DisplayName("分页查询活动 - 只读取当前页并返回总数")
    void testQueryPageCached_CacheMiss_QueryCurrentPage() {
        Activity activity = createActivity(20);
        when(activityMapper.queryPage("AI", 10, 10)).thenReturn(Collections.singletonList(activity));
        when(activityMapper.countQuery("AI")).thenReturn(51L);

        PageResult<Activity> page = activityService.queryPageCached("AI", 2, 10);

        assertEquals(1, page.getRecords().size());
        assertEquals(51L, page.getTotal());
        assertEquals(6, page.getPages());
        verify(activityMapper).queryPage("AI", 10, 10);
    }

    @Test
    @DisplayName("查询活动列表缓存 - 缓存未命中时读取数据库")
    void testQueryCached_CacheMiss_QueryMapper() {
        // Given - Redis is unavailable in unit tests, so cached query falls back to mapper
        Activity activity = createActivity(20);
        when(activityMapper.query("AI")).thenReturn(Collections.singletonList(activity));

        // When - querying cached activity list
        java.util.List<Activity> result = activityService.queryCached("AI");

        // Then - mapper result is returned and no external Redis server is required
        assertEquals(1, result.size());
        assertSame(activity, result.get(0));
        verify(activityMapper).query("AI");
    }

    @Test
    @DisplayName("查询活动详情缓存 - 缓存未命中时读取数据库")
    void testGetCachedActivityDetail_CacheMiss_QueryMapper() {
        // Given - no cache value exists
        Activity activity = createActivity(30);
        when(activityMapper.getActivityByNameAndGroupName("迎新活动", "计算机协会")).thenReturn(activity);

        // When - reading cached detail
        Activity result = activityService.getCachedActivityDetail("迎新活动", "计算机协会");

        // Then - service returns database detail
        assertSame(activity, result);
        verify(activityMapper).getActivityByNameAndGroupName("迎新活动", "计算机协会");
    }

    @Test
    @DisplayName("查询活动附件 - 活动不存在时抛出业务异常")
    void testGetAttachment_ActivityNotFound_ThrowException() {
        // Given - activity id does not exist
        when(activityMapper.getActivityById(99)).thenReturn(null);

        // When & Then - missing attachment target is reported explicitly
        BusinessException exception = assertThrows(BusinessException.class, () -> activityService.getAttachment(99));
        assertEquals("ACTIVITY_NOT_EXIST", exception.getCode());
    }

    @Test
    @DisplayName("审核活动申请 - 通过时清理报名保护和活动缓存")
    void testConfirmApplication_Success_ClearApplyGuardAndCache() {
        // When - approving activity application
        activityService.confirmApplication(20);

        // Then - approval mapper is called and cache eviction fallback is published
        verify(activityMapper).confirmApplication(20);
        verify(kafkaProducer).send(eq(com.chinahitech.shop.constant.MqTopics.ACTIVITY_CACHE), anyString());
    }

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

    @Test
    @DisplayName("发布活动 - 活动信息为空时拒绝保存")
    void testInsert_NullActivity_ThrowException() {
        // Given - 准备空活动对象，模拟前端误提交或接口被错误调用。
        Activity activity = null;

        // When & Then - 服务层应直接拒绝请求，避免 Mapper 写入脏数据。
        assertThrows(BusinessException.class, () -> activityService.insert(activity));
        verify(activityMapper, never()).insert(any(Activity.class));
    }

    @Test
    @DisplayName("发布活动 - 缺少活动名称时拒绝保存")
    void testInsert_BlankName_ThrowException() {
        // Given - 活动缺少名称，无法在列表和详情中形成可识别的业务记录。
        Activity activity = createSavableActivity();
        activity.setName(" ");

        // When & Then - 校验失败后不进入数据库保存流程。
        assertThrows(BusinessException.class, () -> activityService.insert(activity));
        verify(activityMapper, never()).insert(any(Activity.class));
    }

    @Test
    @DisplayName("发布活动 - 信息完整时写入数据库")
    void testInsert_ValidActivity_SaveSuccess() {
        // Given - 准备活动发布所需的最小有效字段。
        Activity activity = createSavableActivity();

        // When - 执行活动发布保存逻辑。
        activityService.insert(activity);

        // Then - 只在校验通过后调用 Mapper 写入数据库。
        verify(activityMapper).insert(activity);
    }

    @Test
    @DisplayName("修改活动 - 缺少活动编号时拒绝保存")
    void testModifyInfo_MissingId_ThrowException() {
        // Given - 修改操作必须依赖活动编号定位原记录。
        Activity activity = createSavableActivity();
        activity.setId(0);

        // When & Then - 缺少编号时应阻止更新，避免 SQL 无效或误更新。
        assertThrows(BusinessException.class, () -> activityService.modifyInfo(activity));
        verify(activityMapper, never()).modifyInfo(any(Activity.class));
    }

    @Test
    @DisplayName("修改活动 - 信息完整时更新数据库")
    void testModifyInfo_ValidActivity_UpdateSuccess() {
        // Given - 准备带有活动编号和归属社团的有效修改数据。
        Activity activity = createSavableActivity();
        activity.setId(20);

        // When - 执行活动信息修改。
        activityService.modifyInfo(activity);

        // Then - 校验通过后调用 Mapper 更新指定活动。
        verify(activityMapper).modifyInfo(activity);
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

    private Activity createSavableActivity() {
        Activity activity = new Activity();
        activity.setId(1);
        activity.setName("校园 AI 分享会");
        activity.setGroupName("AI 创新社");
        activity.setOrganizer("clubadmin");
        activity.setNumber(80);
        return activity;
    }
}
