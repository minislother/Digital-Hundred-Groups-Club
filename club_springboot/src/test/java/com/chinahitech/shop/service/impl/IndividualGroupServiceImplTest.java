package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.IndividualGroupMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.service.GroupService;
import com.chinahitech.shop.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("个人社团服务单元测试")
@ExtendWith(MockitoExtension.class)
class IndividualGroupServiceImplTest {

    @Mock
    private IndividualGroupMapper individualGroupMapper;

    @Mock
    private GroupService groupService;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private IndividualGroupServiceImpl individualGroupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", redisTemplate);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", null);
    }

    @Test
    @DisplayName("查询学生社团 - 缓存未命中时组装学生和社团列表")
    void testGetStudentGroupsCached_CacheMiss_ReturnCombinedMap() {
        // Given - cache miss and joined group
        IndividualGroup individualGroup = new IndividualGroup();
        individualGroup.setGroupId(7);
        Group group = new Group();
        group.setId(7);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("individualGroup:student:1001")).thenReturn(null);
        when(individualGroupMapper.getGroupByStuId("1001")).thenReturn(Collections.singletonList(individualGroup));
        when(groupService.getGroupById(7)).thenReturn(group);

        // When - querying student groups
        Map<String, Object> result = individualGroupService.getStudentGroupsCached("1001");

        // Then - relation and group detail are returned
        assertEquals(1, ((java.util.List<?>) result.get("stuItems")).size());
        assertSame(group, ((java.util.List<?>) result.get("groupItems")).get(0));
        verify(individualGroupMapper).getGroupByStuId("1001");
        verify(groupService).getGroupById(7);
    }

    @Test
    @DisplayName("申请加入社团 - 成功写入待审核记录并发送通知")
    void testApplyJoinGroupAndNotify_Success_InsertPendingRecordAndSendEvent() {
        // Given - no existing application and Redis guard passes
        when(individualGroupMapper.checkExist("1001", 7)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        ArgumentCaptor<IndividualGroup> captor = ArgumentCaptor.forClass(IndividualGroup.class);

        // When - applying to join group
        individualGroupService.applyJoinGroupAndNotify(7, "1001");

        // Then - pending record is inserted and event is sent
        verify(individualGroupMapper).insert(captor.capture());
        IndividualGroup inserted = captor.getValue();
        assertEquals(7, inserted.getGroupId());
        assertEquals("1001", inserted.getStudentId());
        assertEquals(0, inserted.getStatus());
        assertEquals("成员", inserted.getPosition());
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_GROUP), contains("join_group_apply"));
    }

    @Test
    @DisplayName("申请加入社团 - Redis防重失败抛出异常")
    void testApplyJoinGroupAndNotify_GuardRejected_ThrowException() {
        // Given - no database record but Redis guard rejects
        when(individualGroupMapper.checkExist("1001", 7)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);

        // When & Then - duplicate application is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.applyJoinGroupAndNotify(7, "1001")
        );
        assertEquals("APPLY_EXIST", exception.getCode());
        verify(individualGroupMapper, never()).insert(any(IndividualGroup.class));
    }

    @Test
    @DisplayName("申请加入社团 - 数据库已存在时不写入Redis guard")
    void testApplyJoinGroupAndNotify_AlreadyExists_ThrowException() {
        // Given - existing application or member record
        when(individualGroupMapper.checkExist("1001", 7)).thenReturn(new IndividualGroup());

        // When & Then - duplicate application is rejected before Redis guard
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.applyJoinGroupAndNotify(7, "1001")
        );
        assertEquals("APPLY_EXIST", exception.getCode());
        verify(redisTemplate, never()).opsForValue();
        verify(individualGroupMapper, never()).insert(any(IndividualGroup.class));
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_GROUP), any(String.class));
    }

    @Test
    @DisplayName("审批入团申请 - 通过时更新状态并发送通知")
    void testAcceptJoinAndNotify_Success_UpdateStatusAndSendEvent() {
        // Given - update succeeds
        when(individualGroupMapper.updateStatus(7, "1001", 1)).thenReturn(1);

        // When - approving join application
        individualGroupService.acceptJoinAndNotify(7, "1001");

        // Then - status is updated and event is sent
        verify(individualGroupMapper).updateStatus(7, "1001", 1);
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_GROUP), contains("accept_group_apply"));
    }

    @Test
    @DisplayName("审批入团申请 - 非社团管理员抛出异常")
    void testAcceptJoinAndNotify_NotManaged_ThrowException() {
        // Given - current manager does not manage group
        when(individualGroupMapper.countManagedGroup(7, "manager002")).thenReturn(0);

        // When & Then - unauthorized manager is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.acceptJoinAndNotify(7, "1001", "manager002")
        );
        assertEquals("FORBIDDEN", exception.getCode());
        verify(individualGroupMapper, never()).updateStatus(7, "1001", 1);
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_GROUP), any(String.class));
    }

    @Test
    @DisplayName("审批入团申请 - 更新0行抛出异常")
    void testAcceptJoinAndNotify_NoRowsUpdated_ThrowException() {
        // Given - update affects no rows
        when(individualGroupMapper.updateStatus(7, "1001", 1)).thenReturn(0);

        // When & Then - missing application is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.acceptJoinAndNotify(7, "1001")
        );
        assertEquals("NOT_FOUND", exception.getCode());
        verify(kafkaProducer, never()).send(eq(MqTopics.INDIVIDUAL_GROUP), any(String.class));
    }

    @Test
    @DisplayName("转让社团管理权 - 成功更新双方状态并发送通知")
    void testTransferStatusAndNotify_Success_UpdateBothManagersAndSendEvent() {
        // Given - current manager owns group and both updates succeed
        when(individualGroupMapper.countManagedGroup(7, "manager001")).thenReturn(1);
        when(individualGroupMapper.updateStatus(7, "manager001", 0)).thenReturn(1);
        when(individualGroupMapper.updateStatus(7, "manager002", 2)).thenReturn(1);

        // When - transferring manager status
        individualGroupService.transferStatusAndNotify(7, "manager001", "manager002");

        // Then - old manager is downgraded and new manager is promoted
        verify(individualGroupMapper).updateStatus(7, "manager001", 0);
        verify(individualGroupMapper).updateStatus(7, "manager002", 2);
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_GROUP), contains("transfer_manager"));
    }

    @Test
    @DisplayName("修改成员权限 - 状态参数写入并发送通知")
    void testUpdatePermissionAndNotify_Success_UpdateStatusAndSendEvent() {
        // Given - update succeeds
        when(individualGroupMapper.updateStatus(7, "1001", 2)).thenReturn(1);

        // When - updating member permission
        individualGroupService.updatePermissionAndNotify(7, "1001", 2);

        // Then - status is written to mapper
        verify(individualGroupMapper).updateStatus(7, "1001", 2);
        verify(kafkaProducer).send(eq(MqTopics.INDIVIDUAL_GROUP), contains("update_permission"));
    }

    @Test
    @DisplayName("管理社团列表 - 管理员ID为空抛出异常")
    void testGetAllManagedGroupsCached_BlankManagerId_ThrowException() {
        // When & Then - blank manager id is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.getAllManagedGroupsCached(" ")
        );
        assertEquals("PARAM_ERROR", exception.getCode());
        verify(individualGroupMapper, never()).getAllManagedGroups(" ");
    }

    @Test
    @DisplayName("查询社团申请列表 - 社团ID非法抛出异常")
    void testGetGroupApplyListCached_InvalidGroupId_ThrowException() {
        // When & Then - invalid group id is rejected
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> individualGroupService.getGroupApplyListCached(0)
        );
        assertEquals("PARAM_ERROR", exception.getCode());
        verify(individualGroupMapper, never()).getGroupApplyList(0);
    }

    @Test
    @DisplayName("管理社团列表 - 缓存未命中时查询Mapper")
    void testGetAllManagedGroupsCached_CacheMiss_QueryMapper() {
        // Given - cache miss and one managed group
        Group group = new Group();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("individualGroup:manager:manager001")).thenReturn(null);
        when(individualGroupMapper.getAllManagedGroups("manager001")).thenReturn(Collections.singletonList(group));

        // When - querying managed groups
        java.util.List<Group> result = individualGroupService.getAllManagedGroupsCached("manager001");

        // Then - mapper result is returned
        assertEquals(1, result.size());
        assertSame(group, result.get(0));
        verify(individualGroupMapper).getAllManagedGroups("manager001");
    }
}
