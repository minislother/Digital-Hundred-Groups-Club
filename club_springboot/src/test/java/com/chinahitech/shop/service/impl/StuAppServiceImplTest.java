package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.StuApp;
import com.chinahitech.shop.exception.ApplyException;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.StuAppMapper;
import com.chinahitech.shop.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("学生入团申请服务单元测试")
@ExtendWith(MockitoExtension.class)
class StuAppServiceImplTest {

    @Mock
    private StuAppMapper stuAppMapper;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private StuAppServiceImpl stuAppService;

    @Test
    @DisplayName("查询我的申请 - 返回Mapper查询结果")
    void testQueryMyapp_Success_ReturnMapperResult() {
        // Given - mapper returns one application
        StuApp app = application();
        when(stuAppMapper.findMyapp("20240001")).thenReturn(Collections.singletonList(app));

        // When - querying applications of current student
        java.util.List<StuApp> result = stuAppService.queryMyapp("20240001");

        // Then - service exposes mapper result
        assertEquals(1, result.size());
        assertEquals("摄影社", result.get(0).getGroupName());
    }

    @Test
    @DisplayName("新增申请 - 空申请对象抛出申请异常")
    void testInsert_NullApplication_ThrowException() {
        // When & Then - null request is rejected before mapper interaction
        assertThrows(ApplyException.class, () -> stuAppService.insert(null));
        verify(stuAppMapper, never()).insert(any(StuApp.class));
    }

    @Test
    @DisplayName("新增申请 - 历史申请达到上限时拒绝提交")
    void testInsert_TooManyApplications_ThrowException() {
        // Given - the same student has five records for the same group
        StuApp app = application();
        when(stuAppMapper.getByStuAndGroup("20240001", "摄影社"))
                .thenReturn(Arrays.asList(new StuApp(), new StuApp(), new StuApp(), new StuApp(), new StuApp()));

        // When & Then - duplicate/frequent application is rejected
        assertThrows(ApplyException.class, () -> stuAppService.insert(app));
        verify(stuAppMapper, never()).insert(any(StuApp.class));
    }

    @Test
    @DisplayName("新增申请 - 数据库写入失败时抛出新增异常")
    void testInsert_InsertFailed_ThrowException() {
        // Given - mapper update count indicates failure
        StuApp app = application();
        when(stuAppMapper.getByStuAndGroup("20240001", "摄影社")).thenReturn(Collections.emptyList());
        when(stuAppMapper.insert(app)).thenReturn(0);

        // When & Then - service converts failed write to business exception
        assertThrows(InsertException.class, () -> stuAppService.insert(app));
    }

    @Test
    @DisplayName("提交申请 - 写入创建时间并增加社团热度")
    void testSubmit_Success_SetCreateTimeAndAddHot() {
        // Given - new application can be inserted
        StuApp app = application();
        when(stuAppMapper.getByStuAndGroup("20240001", "摄影社")).thenReturn(null);
        when(stuAppMapper.insert(app)).thenReturn(1);

        // When - submitting application
        stuAppService.submit(app);

        // Then - timestamp is filled and group hot value is updated
        assertNotNull(app.getCreateTime());
        verify(stuAppMapper).insert(app);
        verify(groupService).addHot("摄影社");
    }

    @Test
    @DisplayName("更新附件 - 申请不存在时抛出实体不存在异常")
    void testUpdateAttachment_NotFound_ThrowException() {
        // Given - application id is missing
        when(stuAppMapper.getById(77)).thenReturn(null);

        // When & Then - missing application no longer causes NullPointerException
        assertThrows(EntityNotFoundException.class, () -> stuAppService.updateAttachment(77, "uploads/a.pdf"));
        verify(stuAppMapper, never()).updateById(any(StuApp.class));
    }

    @Test
    @DisplayName("确认入团申请 - 成功时写入通过状态")
    void testConfirmApplication_Success_SetAcceptedTrue() {
        // Given - existing application and successful update
        StuApp app = application();
        when(stuAppMapper.getById(7)).thenReturn(app);
        when(stuAppMapper.updateById(app)).thenReturn(1);

        // When - approving application
        stuAppService.confirmApplication(7);

        // Then - accepted flag is set to true
        assertTrue(app.getIsAccepted());
        verify(stuAppMapper).updateById(app);
    }

    @Test
    @DisplayName("拒绝入团申请 - 更新0行时抛出修改异常")
    void testDenyApplication_UpdateFailed_ThrowException() {
        // Given - existing application but update fails
        StuApp app = application();
        when(stuAppMapper.getById(7)).thenReturn(app);
        when(stuAppMapper.updateById(app)).thenReturn(0);

        // When & Then - failed status write is reported
        assertThrows(UpdateException.class, () -> stuAppService.denyApplication(7));
        assertFalse(app.getIsAccepted());
    }

    @Test
    @DisplayName("更新附件 - 成功时保存新的附件地址")
    void testUpdateAttachment_Success_UpdateAttachment() {
        // Given - existing application and successful update
        StuApp app = application();
        when(stuAppMapper.getById(7)).thenReturn(app);
        when(stuAppMapper.updateById(any(StuApp.class))).thenReturn(1);
        ArgumentCaptor<StuApp> appCaptor = ArgumentCaptor.forClass(StuApp.class);

        // When - updating attachment path
        stuAppService.updateAttachment(7, "uploads/new.pdf");

        // Then - mapper receives application with new attachment
        verify(stuAppMapper).updateById(appCaptor.capture());
        assertEquals("uploads/new.pdf", appCaptor.getValue().getAttachment());
    }

    private StuApp application() {
        StuApp app = new StuApp();
        app.setApplicationId(7);
        app.setStuNumber("20240001");
        app.setGroupName("摄影社");
        return app;
    }
}
