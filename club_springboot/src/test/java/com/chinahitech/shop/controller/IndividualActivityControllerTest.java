package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.IndividualActivityService;
import com.chinahitech.shop.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("个人活动控制器单元测试")
@ExtendWith(MockitoExtension.class)
class IndividualActivityControllerTest {

    @Mock
    private IndividualActivityService individualActivityService;

    @InjectMocks
    private IndividualActivityController individualActivityController;

    @Test
    @DisplayName("学生活动列表 - 使用认证学生编号")
    void testGetIndividualActivity_AuthenticatedUser_CallServiceWithPrincipal() {
        // Given - authenticated student
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);
        when(individualActivityService.getActivityByStuIdCached("1001")).thenReturn(Collections.emptyList());

        // When - querying joined activities
        Result result = individualActivityController.getIndividualActivity(1, 10, authentication);

        // Then - service receives principal from authentication
        assertTrue(result.getSuccess());
        verify(individualActivityService).getActivityByStuIdCached("1001");
    }

    @Test
    @DisplayName("学生活动列表 - 缺少认证抛出异常")
    void testGetIndividualActivity_MissingAuthentication_ThrowException() {
        // When & Then - unauthenticated request is rejected before service call
        assertThrows(RuntimeException.class, () -> individualActivityController.getIndividualActivity(1, 10, null));
        verify(individualActivityService, never()).getActivityByStuIdCached("1001");
    }

    @Test
    @DisplayName("申请加入活动 - 使用认证学生编号")
    void testJoinActivity_AuthenticatedUser_CallServiceWithPrincipal() {
        // Given - authenticated student and activity id
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When - joining an activity
        Result result = individualActivityController.joinActivity(9, authentication);

        // Then - service receives principal from authentication
        assertTrue(result.getSuccess());
        verify(individualActivityService).joinActivityAndNotify(9, "1001");
    }

    @Test
    @DisplayName("批准活动申请 - 使用认证管理员编号")
    void testAcceptApplication_ManagerEndpoint_CallServiceWithAuthenticatedManager() {
        // Given - authenticated manager
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);

        // When - manager accepts an activity application
        Result result = individualActivityController.acceptApplication(9, "1001", authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualActivityService).confirmApplicationAndNotify(9, "1001", "manager001");
    }

    @Test
    @DisplayName("拒绝活动申请 - 使用认证管理员编号")
    void testRejectApplication_ManagerEndpoint_CallServiceWithAuthenticatedManager() {
        // Given - authenticated manager
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);

        // When - manager rejects an activity application
        Result result = individualActivityController.rejectApplication(9, "1001", authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualActivityService).denyApplicationAndNotify(9, "1001", "manager001");
    }

    @Test
    @DisplayName("管理活动列表 - 使用认证管理员编号")
    void testGetAllManagedActivities_AuthenticatedManagerId_CallService() {
        // Given - authenticated manager and empty managed activity list
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);
        when(individualActivityService.getAllManagedActivitiesCached("manager001"))
                .thenReturn(Collections.emptyList());

        // When - manager queries managed activities
        Result result = individualActivityController.getAllManagedActivities(1, 10, authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualActivityService).getAllManagedActivitiesCached("manager001");
    }
}
