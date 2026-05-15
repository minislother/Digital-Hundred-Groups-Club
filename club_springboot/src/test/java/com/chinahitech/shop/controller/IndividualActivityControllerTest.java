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

@DisplayName("IndividualActivityController unit tests")
@ExtendWith(MockitoExtension.class)
class IndividualActivityControllerTest {

    @Mock
    private IndividualActivityService individualActivityService;

    @InjectMocks
    private IndividualActivityController individualActivityController;

    @Test
    @DisplayName("allActivities - authenticated user id is used")
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
    @DisplayName("allActivities - missing authentication throws exception")
    void testGetIndividualActivity_MissingAuthentication_ThrowException() {
        // When & Then - unauthenticated request is rejected before service call
        assertThrows(RuntimeException.class, () -> individualActivityController.getIndividualActivity(1, 10, null));
        verify(individualActivityService, never()).getActivityByStuIdCached("1001");
    }

    @Test
    @DisplayName("joinActivity - authenticated user id is used")
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
    @DisplayName("accept - manager endpoint delegates request parameters")
    void testAcceptApplication_ManagerEndpoint_CallService() {
        // When - manager accepts an activity application
        Result result = individualActivityController.acceptApplication(9, "1001");

        // Then - service receives request parameters
        assertTrue(result.getSuccess());
        verify(individualActivityService).confirmApplicationAndNotify(9, "1001");
    }

    @Test
    @DisplayName("reject - manager endpoint delegates request parameters")
    void testRejectApplication_ManagerEndpoint_CallService() {
        // When - manager rejects an activity application
        Result result = individualActivityController.rejectApplication(9, "1001");

        // Then - service receives request parameters
        assertTrue(result.getSuccess());
        verify(individualActivityService).denyApplicationAndNotify(9, "1001");
    }

    @Test
    @DisplayName("管理活动列表 - 使用请求中的管理员编号查询")
    void testGetAllManagedActivities_RequestManagerId_CallService() {
        // Given - manager id and empty managed activity list
        when(individualActivityService.getAllManagedActivitiesCached("manager001"))
                .thenReturn(Collections.emptyList());

        // When - manager queries managed activities
        Result result = individualActivityController.getAllManagedActivities("manager001", 1, 10);

        // Then - service receives requested manager id
        assertTrue(result.getSuccess());
        verify(individualActivityService).getAllManagedActivitiesCached("manager001");
    }
}
