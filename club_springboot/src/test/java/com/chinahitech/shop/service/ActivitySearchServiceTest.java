package com.chinahitech.shop.service;

import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mapper.ActivitySearchOutboxMapper;
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.Result;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;

@DisplayName("活动搜索 - 分页边界与降级单元测试")
@ExtendWith(MockitoExtension.class)
class ActivitySearchServiceTest {
    @Mock ActivityMapper activityMapper;
    @Mock ActivitySearchOutboxMapper outbox;
    @Mock FileStorageService fileStorageService;
    @Mock RestHighLevelClient client;
    @InjectMocks ActivitySearchService search;

    @Test @DisplayName("未启用ES - SQL分页并返回真实总数及降级说明")
    void testSearch_Disabled_PagedFallback() {
        // Given
        ReflectionTestUtils.setField(search, "client", null);
        when(activityMapper.queryPage("协会", 100, 20)).thenReturn(Collections.emptyList());
        when(activityMapper.countQuery("协会")).thenReturn(250L);
        // When
        Result result = search.searchPage("协会", 6, 20);
        // Then
        assertEquals("mysql", result.getData().get("searchSource"));
        assertEquals(true, result.getData().get("degraded"));
        assertEquals(250L, ((PageResult<?>) result.getData().get("page")).getTotal());
        verify(activityMapper, never()).query(any());
    }

    @Test @DisplayName("ES初始化失败 - 显式降级且不伪造ES命中")
    void testSearch_Unavailable_Fallback() {
        // Given
        when(client.indices()).thenThrow(new IllegalStateException("connection refused"));
        when(activityMapper.queryPage("人工智能", 0, 10)).thenReturn(Collections.emptyList());
        // When
        Result result = search.searchPage("人工智能", null, null);
        // Then
        assertEquals(true, result.getData().get("degraded"));
        verify(activityMapper).countQuery("人工智能");
    }

    @Test @DisplayName("非法页码与深分页 - 查询前拒绝并防止整数溢出")
    void testSearch_InvalidPage_Reject() {
        // Given / When / Then
        assertThrows(BusinessException.class, () -> search.searchPage("词", 0, 10));
        assertThrows(BusinessException.class, () -> search.searchPage("词", 1, 101));
        assertThrows(BusinessException.class, () -> search.searchPage("词", Integer.MAX_VALUE, 100));
        assertThrows(BusinessException.class, () -> search.searchPage("字".repeat(201), 1, 10));
        verifyNoInteractions(client, activityMapper);
    }

    @Test @DisplayName("回填 - 仅返回持久化排队数")
    void testRebuild_QueueOnly() {
        // Given
        when(outbox.enqueueAll()).thenReturn(125);
        // When
        int queued = search.rebuild();
        // Then
        assertEquals(125, queued);
        verifyNoInteractions(client, activityMapper);
    }

    @Test @DisplayName("未启用ES - 不把回填伪装为成功")
    void testRebuild_Disabled_Reject() {
        // Given
        ReflectionTestUtils.setField(search, "client", null);
        // When / Then
        assertThrows(BusinessException.class, () -> search.rebuild());
        verifyNoInteractions(outbox);
    }

    @Test @DisplayName("游标降级 - 多取一条并拒绝篡改和更换关键词")
    void testSearchSlice_MysqlCursor_NoCountAndRejectsTampering() {
        // Given
        ReflectionTestUtils.setField(search, "client", null);
        ReflectionTestUtils.setField(search, "cursorSecret", "test-only-search-cursor-key-with-64-characters-padding-padding-padding");
        var first = new com.chinahitech.shop.bean.Activity(); first.setId(30);
        var sentinel = new com.chinahitech.shop.bean.Activity(); sentinel.setId(20);
        when(activityMapper.searchSlice("协会", null, 2)).thenReturn(java.util.List.of(first, sentinel));
        when(activityMapper.searchSlice("协会", 30L, 2)).thenReturn(java.util.List.of(sentinel));

        // When
        var response = search.searchSlice("协会", 1, null);
        String cursor = (String) response.getData().get("nextCursor");
        var second = search.searchSlice("协会", 1, cursor);

        // Then
        assertEquals(true, response.getData().get("hasNext"));
        assertEquals(false, second.getData().get("hasNext"));
        assertThrows(BusinessException.class, () -> search.searchSlice("其他", 1, cursor));
        assertThrows(BusinessException.class, () -> search.searchSlice("协会", 1, cursor + "bad"));
        verify(activityMapper, never()).countQuery(any());
    }

    @Test @DisplayName("ES游标 - 使用PIT固定视图且结束时关闭")
    void testSearchSlice_ElasticsearchPit_ClosesCompletedView() throws Exception {
        // Given
        ReflectionTestUtils.setField(search, "initialized", true);
        ReflectionTestUtils.setField(search, "cursorSecret", "test-only-search-cursor-key-with-64-characters-padding-padding-padding");
        var opened = mock(org.elasticsearch.action.search.OpenPointInTimeResponse.class);
        when(opened.getPointInTimeId()).thenReturn("pit-1");
        when(client.openPointInTime(any(), any())).thenReturn(opened);
        var response = mock(org.elasticsearch.action.search.SearchResponse.class);
        when(response.getHits()).thenReturn(new org.elasticsearch.search.SearchHits(
                new org.elasticsearch.search.SearchHit[0], null, Float.NaN));
        when(response.pointInTimeId()).thenReturn("pit-2");
        when(client.search(any(), any())).thenReturn(response);

        // When
        var result = search.searchSlice("协会", 20, null);

        // Then
        assertEquals(false, result.getData().get("hasNext"));
        var request = ArgumentCaptor.forClass(org.elasticsearch.action.search.SearchRequest.class);
        verify(client).search(request.capture(), any());
        assertNotNull(request.getValue().source().pointInTimeBuilder());
        assertEquals(21, request.getValue().source().size());
        assertEquals(0, request.getValue().indices().length);
        verify(client).closePointInTime(any(), any());
        verifyNoInteractions(activityMapper);
    }
}
