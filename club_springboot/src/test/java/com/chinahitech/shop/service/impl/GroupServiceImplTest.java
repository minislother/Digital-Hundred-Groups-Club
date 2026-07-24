package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.GroupMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.utils.RedisUtils;
import com.chinahitech.shop.utils.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("社团服务单元测试")
@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private GroupServiceImpl groupService;

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(RedisUtils.class, "redisTemplate", null);
    }

    @Test
    @DisplayName("查询社团列表 - 直接委托Mapper返回结果")
    void testQuery_WithSearchInfo_ReturnMapperResult() {
        // Given - mapper returns one matched group
        Group group = group("摄影社");
        when(groupMapper.query("摄影")).thenReturn(Collections.singletonList(group));

        // When - querying by keyword
        List<Group> result = groupService.query("摄影");

        // Then - service returns mapper data without extra transformation
        assertEquals(1, result.size());
        assertSame(group, result.get(0));
        verify(groupMapper).query("摄影");
    }

    @Test
    @DisplayName("分页查询社团 - 只读取当前页并返回总数")
    void testQueryPageCached_CacheMiss_QueryCurrentPage() {
        Group group = group("摄影社");
        when(groupMapper.queryPage("摄影", 20, 10)).thenReturn(Collections.singletonList(group));
        when(groupMapper.countQuery("摄影")).thenReturn(35L);

        PageResult<Group> page = groupService.queryPageCached("摄影", 3, 10);

        assertEquals(1, page.getRecords().size());
        assertEquals(35L, page.getTotal());
        assertEquals(3, page.getPageNum());
        verify(groupMapper).queryPage("摄影", 20, 10);
    }

    @Test
    @DisplayName("查询社团详情 - 社团不存在时抛出业务异常")
    void testGetGroupById_NotFound_ThrowException() {
        // Given - no group record exists
        when(groupMapper.getGroupById(99)).thenReturn(null);

        // When & Then - missing group is reported as business error
        BusinessException exception = assertThrows(BusinessException.class, () -> groupService.getGroupById(99));
        assertEquals("GROUP_NOT_EXIST", exception.getCode());
    }

    @Test
    @DisplayName("查询附件 - 社团存在时返回附件地址")
    void testGetAttachment_GroupExists_ReturnAttachment() {
        // Given - group has uploaded attachment
        Group group = group("书法社");
        group.setAttachment("uploads/group.docx");
        when(groupMapper.getGroupById(7)).thenReturn(group);

        // When - reading attachment path
        String attachment = groupService.getAttachment(7);

        // Then - attachment is returned from group entity
        assertEquals("uploads/group.docx", attachment);
    }

    @Test
    @DisplayName("新增社团申请 - 清空审核状态并发送审核消息")
    void testInsert_Success_ResetAcceptedAndSendEvent() {
        // Given - a new group application contains a stale accepted flag
        Group group = group("篮球社");
        group.setIsAccepted(Boolean.TRUE);

        // When - inserting application
        groupService.insert(group);

        // Then - status is reset for super-admin review and message is sent
        assertNull(group.getIsAccepted());
        verify(groupMapper).insert(group);
        verify(kafkaProducer).send(eq(MqTopics.GROUP), contains("addGroup"));
    }

    @Test
    @DisplayName("查询社团详情缓存 - 缓存未命中时读取数据库")
    void testGetCachedGroupDetail_CacheMiss_QueryMapper() {
        // Given - Redis is unavailable in unit tests, so service falls back to mapper
        Group group = group("舞蹈社");
        when(groupMapper.getByName("舞蹈社")).thenReturn(group);

        // When - reading cached detail
        Group result = groupService.getCachedGroupDetail("舞蹈社");

        // Then - mapper result is returned
        assertSame(group, result);
        verify(groupMapper).getByName("舞蹈社");
    }

    @Test
    @DisplayName("审核社团申请 - 通过时更新状态并触发缓存清理消息")
    void testConfirmApplication_Success_UpdateAndEvictCache() {
        // When - approving group application
        groupService.confirmApplication(7);

        // Then - database status is updated and cache eviction fallback is published
        verify(groupMapper).confirmApplication(7);
        verify(kafkaProducer).send(eq(MqTopics.GROUP_CACHE), contains("group_cache_evict"));
    }

    @Test
    @DisplayName("修改社团图片 - 更新数据库并触发缓存清理")
    void testUpdateImage_Success_UpdateAndEvictCache() {
        // When - changing group cover image
        groupService.updateImage("摄影社", "uploads/image.png");

        // Then - mapper receives exact update arguments
        verify(groupMapper).updateImage("摄影社", "uploads/image.png");
        verify(kafkaProducer).send(eq(MqTopics.GROUP_CACHE), anyString());
    }

    private Group group(String name) {
        Group group = new Group();
        group.setId(7);
        group.setName(name);
        return group;
    }
}
