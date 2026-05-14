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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ActivityController unit tests")
@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ActivityController activityController;

    @Test
    @DisplayName("applyJoin - authenticated student id is used")
    void testApplyJoin_Success_ReturnOk() {
        // Given - authenticated user and activity id
        Integer activityId = 1;
        Integer studentId = 1001;
        Authentication authentication = new UsernamePasswordAuthenticationToken(studentId.toString(), null);

        // When - applying for activity
        Result result = activityController.applyJoin(activityId, authentication);

        // Then - service receives student id from authentication
        assertTrue(result.getSuccess());
        verify(activityService).applyJoin(activityId, studentId);
    }

    @Test
    @DisplayName("applyJoin - null activity id throws exception")
    void testApplyJoin_NullActivityId_ThrowException() {
        // Given - authenticated user
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When & Then - null activity id is rejected
        assertThrows(RuntimeException.class, () -> activityController.applyJoin(null, authentication));
        verify(activityService, never()).applyJoin(null, 1001);
    }

    @Test
    @DisplayName("applyJoin - non numeric principal throws exception")
    void testApplyJoin_NonNumericPrincipal_ThrowException() {
        // Given - authenticated user with non-numeric id
        Authentication authentication = new UsernamePasswordAuthenticationToken("student-a", null);

        // When & Then - non-numeric id is rejected before service call
        assertThrows(RuntimeException.class, () -> activityController.applyJoin(1, authentication));
        verify(activityService, never()).applyJoin(1, null);
    }

    @Test
    @DisplayName("myJoinedActivities - authenticated student id is used")
    void testMyJoinedActivities_AuthenticatedUser_CallService() {
        // Given - authenticated user
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When - querying joined activities
        activityController.myJoinedActivities(1, 10, authentication);

        // Then - service receives student id from authentication
        verify(activityService).getMyJoinedActivities(1001);
    }

    @Test
    @DisplayName("managerDetail - forwards activity and group name")
    void testGetManagerDetail_WithActivityAndGroupName_CallServiceWithBothParams() {
        // Given - activity and group names
        String activityName = "welcome";
        String groupName = "computer";
        when(activityService.getActivityByNameAndGroupName(activityName, groupName))
                .thenReturn(new com.chinahitech.shop.bean.Activity());

        // When - querying manager detail
        Result result = activityController.getManagerDetail(activityName, groupName);

        // Then - arguments are forwarded in order
        assertTrue(result.getSuccess());
        verify(activityService).getActivityByNameAndGroupName(activityName, groupName);
        verify(activityService, never()).getActivityByNameAndGroupName(groupName, groupName);
    }

    @Test
    @DisplayName("managerDetail - null activity name throws exception")
    void testGetManagerDetail_NullParam_ThrowException() {
        // When & Then - invalid input is rejected
        assertThrows(RuntimeException.class, () -> activityController.getManagerDetail(null, "computer"));
        verify(activityService, never()).getActivityByNameAndGroupName(null, "computer");
    }

    @Test
    @DisplayName("modifyDescription - forwards all fields")
    void testModifyDescription_Success_CallService() {
        // Given - description fields
        String groupName = "computer";
        String activityName = "welcome";
        String description = "activity description";
        String attachment = "activity.zip";
        String image = "cover.png";

        // When - modifying description
        Result result = activityController.modifyDescription(groupName, activityName, description, attachment, image);

        // Then - service receives all fields
        assertTrue(result.getSuccess());
        verify(activityService).updateDescription(groupName, activityName, description, attachment, image);
    }

    @Test
    @DisplayName("modifyDescription - missing activity name throws exception")
    void testModifyDescription_NullActivityName_ThrowException() {
        // When & Then - missing activity name is rejected
        assertThrows(RuntimeException.class,
                () -> activityController.modifyDescription("computer", null, "desc", "activity.zip", "cover.png"));
        verify(activityService, never()).updateDescription("computer", null, "desc", "activity.zip", "cover.png");
    }
}
