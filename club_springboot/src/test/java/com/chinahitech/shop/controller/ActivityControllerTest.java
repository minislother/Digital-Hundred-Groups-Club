package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("活动控制器单元测试")
@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ActivityController activityController;

    @Test
    @DisplayName("申请报名 - 成功返回统一响应")
    void testApplyJoin_Success_ReturnOk() {
        // Given - 准备测试数据
        Integer activityId = 1;
        Integer studentId = 1001;

        // When - 执行被测试方法
        Result result = activityController.applyJoin(activityId, studentId);

        // Then - 验证结果和交互
        assertTrue(result.getSuccess());
        assertEquals("报名申请已提交", result.getMessage());
        verify(activityService).applyJoin(activityId, studentId);
    }

    @Test
    @DisplayName("申请报名 - 参数为空抛出异常")
    void testApplyJoin_NullParam_ThrowException() {
        // Given - 准备测试数据
        Integer studentId = 1001;

        // When & Then - 执行被测试方法并验证异常
        assertThrows(RuntimeException.class, () -> activityController.applyJoin(null, studentId));
        verify(activityService, never()).applyJoin(null, studentId);
    }

    @Test
    @DisplayName("管理端活动详情 - 正确传递活动名和社团名")
    void testGetManagerDetail_WithActivityAndGroupName_CallServiceWithBothParams() {
        // Given - 准备测试数据和Mock行为
        String activityName = "迎新活动";
        String groupName = "计算机协会";
        when(activityService.getActivityByNameAndGroupName(activityName, groupName))
                .thenReturn(new com.chinahitech.shop.bean.Activity());

        // When - 执行被测试方法
        Result result = activityController.getManagerDetail(activityName, groupName);

        // Then - 验证结果和交互
        assertTrue(result.getSuccess());
        verify(activityService).getActivityByNameAndGroupName(activityName, groupName);
        verify(activityService, never()).getActivityByNameAndGroupName(groupName, groupName);
    }

    @Test
    @DisplayName("管理端活动详情 - 参数为空抛出异常")
    void testGetManagerDetail_NullParam_ThrowException() {
        // Given - 准备测试数据
        String groupName = "计算机协会";

        // When & Then - 执行被测试方法并验证异常
        assertThrows(RuntimeException.class, () -> activityController.getManagerDetail(null, groupName));
        verify(activityService, never()).getActivityByNameAndGroupName(null, groupName);
    }

    @Test
    @DisplayName("修改活动描述 - 正确传递社团名和文件信息")
    void testModifyDescription_Success_CallService() {
        // Given - 准备测试数据
        String groupName = "计算机协会";
        String activityName = "迎新活动";
        String description = "活动描述";
        String attachment = "activity.zip";
        String image = "cover.png";

        // When - 执行被测试方法
        Result result = activityController.modifyDescription(groupName, activityName, description, attachment, image);

        // Then - 验证结果和交互
        assertTrue(result.getSuccess());
        verify(activityService).updateDescription(groupName, activityName, description, attachment, image);
    }

    @Test
    @DisplayName("修改活动描述 - 活动名缺失时不调用服务层")
    void testModifyDescription_NullActivityName_ThrowException() {
        String groupName = "计算机协会";

        assertThrows(RuntimeException.class,
                () -> activityController.modifyDescription(groupName, null, "desc", "activity.zip", "cover.png"));
        verify(activityService, never()).updateDescription(groupName, null, "desc", "activity.zip", "cover.png");
    }
}
