package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.IndividualActivityMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("个人活动服务单元测试")
@ExtendWith(MockitoExtension.class)
class IndividualActivityServiceImplTest {

    @Mock
    private IndividualActivityMapper individualActivityMapper;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private IndividualActivityServiceImpl individualActivityService;

    @Test
    @DisplayName("申请加入活动 - 成功写入待审核记录并发送通知")
    void testJoinActivityAndNotify_Success_InsertPendingRecordAndSendEvent() {
        // Given - no existing application
        when(individualActivityMapper.checkExist("1001", 7)).thenReturn(null);
        ArgumentCaptor<IndividualActivity> captor = ArgumentCaptor.forClass(IndividualActivity.class);

        // When - applying to join activity
        individualActivityService.joinActivityAndNotify(7, "1001");

        // Then - pending record is inserted and event is sent
        verify(individualActivityMapper).insert(captor.capture());
        IndividualActivity inserted = captor.getValue();
        assertEquals(7, inserted.getActivityId());
        assertEquals("1001", inserted.getStudentId());
        assertEquals(0, inserted.getStatus());
        assertEquals("成员", inserted.getPosition());
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), contains("join_activity_apply"));
    }

    @Test
    @DisplayName("申请加入活动 - 已存在申请抛出异常")
    void testJoinActivityAndNotify_AlreadyExists_ThrowException() {
        // Given - existing application or member record
        when(individualActivityMapper.checkExist("1001", 7)).thenReturn(new IndividualActivity());

        // When & Then - duplicate application is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualActivityService.joinActivityAndNotify(7, "1001")
        );
        assertEquals("APPLY_EXIST", exception.getCode());
        verify(individualActivityMapper, never()).insert(any(IndividualActivity.class));
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), any(String.class));
    }

    @Test
    @DisplayName("查询活动成员 - 活动ID非法抛出异常")
    void testGetActivityByActivityIdCached_InvalidActivityId_ThrowException() {
        // When & Then - invalid activity id is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualActivityService.getActivityByActivityIdCached(0)
        );
        assertEquals("PARAM_ERROR", exception.getCode());
        verify(individualActivityMapper, never()).getActivityByActivityId(0);
    }

    @Test
    @DisplayName("添加活动成员 - 成功写入已通过状态并发送通知")
    void testAddActivityStudentAndNotify_Success_InsertApprovedRecordAndSendEvent() {
        // Given - member parameters
        ArgumentCaptor<IndividualActivity> captor = ArgumentCaptor.forClass(IndividualActivity.class);

        // When - adding activity member
        individualActivityService.addActivityStudentAndNotify(7, "1001", "摄影");

        // Then - approved record is inserted
        verify(individualActivityMapper).insert(captor.capture());
        IndividualActivity inserted = captor.getValue();
        assertEquals(7, inserted.getActivityId());
        assertEquals("1001", inserted.getStudentId());
        assertEquals(1, inserted.getStatus());
        assertEquals("摄影", inserted.getPosition());
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), contains("add_activity_student"));
    }

    @Test
    @DisplayName("审批活动申请 - 通过时更新状态并发送通知")
    void testConfirmApplicationAndNotify_Success_UpdateStatusAndSendEvent() {
        // Given - update succeeds
        when(individualActivityMapper.updateStatus(7, "1001", 1)).thenReturn(1);

        // When - approving activity application
        individualActivityService.confirmApplicationAndNotify(7, "1001");

        // Then - status is updated and event is sent
        verify(individualActivityMapper).updateStatus(7, "1001", 1);
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), contains("accept_apply"));
    }

    @Test
    @DisplayName("审批活动申请 - 非活动管理员抛出异常")
    void testConfirmApplicationAndNotify_NotManaged_ThrowException() {
        // Given - current manager does not manage activity
        when(individualActivityMapper.countManagedActivity(7, "manager002")).thenReturn(0);

        // When & Then - unauthorized manager is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualActivityService.confirmApplicationAndNotify(7, "1001", "manager002")
        );
        assertEquals("FORBIDDEN", exception.getCode());
        verify(individualActivityMapper, never()).updateStatus(7, "1001", 1);
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), any(String.class));
    }

    @Test
    @DisplayName("审批活动申请 - 更新0行抛出异常")
    void testConfirmApplicationAndNotify_NoRowsUpdated_ThrowException() {
        // Given - update affects no rows
        when(individualActivityMapper.updateStatus(7, "1001", 1)).thenReturn(0);

        // When & Then - missing application is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualActivityService.confirmApplicationAndNotify(7, "1001")
        );
        assertEquals("NOT_FOUND", exception.getCode());
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_ACTIVITY), any(String.class));
    }

    @Test
    @DisplayName("管理活动列表 - 管理员ID为空抛出异常")
    void testGetAllManagedActivitiesCached_BlankManagerId_ThrowException() {
        // When & Then - blank manager id is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualActivityService.getAllManagedActivitiesCached(" ")
        );
        assertEquals("PARAM_ERROR", exception.getCode());
        verify(individualActivityMapper, never()).getAllManagedActivities(" ");
    }

    @Test
    @DisplayName("管理活动列表 - 缓存未命中时查询Mapper")
    void testGetAllManagedActivitiesCached_CacheMiss_QueryMapper() {
        // Given - managed activity data
        Activity activity = new Activity();
        when(individualActivityMapper.getAllManagedActivities("manager001"))
                .thenReturn(Collections.singletonList(activity));

        // When - querying managed activities
        List<Activity> result = individualActivityService.getAllManagedActivitiesCached("manager001");

        // Then - mapper result is returned
        assertEquals(1, result.size());
        assertSame(activity, result.get(0));
        verify(individualActivityMapper).getAllManagedActivities("manager001");
    }
}
