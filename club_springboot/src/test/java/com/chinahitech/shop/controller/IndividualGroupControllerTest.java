package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.IndividualGroupService;
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

@DisplayName("IndividualGroupController unit tests")
@ExtendWith(MockitoExtension.class)
class IndividualGroupControllerTest {

    @Mock
    private IndividualGroupService individualGroupService;

    @InjectMocks
    private IndividualGroupController individualGroupController;

    @Test
    @DisplayName("allGroups - authenticated user id is used")
    void testGetIndividualGroup_AuthenticatedUser_CallServiceWithPrincipal() {
        // Given - authenticated student
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);
        when(individualGroupService.getStudentGroupsCached("1001")).thenReturn(Collections.emptyMap());

        // When - querying student groups
        Result result = individualGroupController.getIndividualGroup(authentication);

        // Then - service receives principal from authentication
        assertTrue(result.getSuccess());
        verify(individualGroupService).getStudentGroupsCached("1001");
    }

    @Test
    @DisplayName("allGroups - missing authentication throws exception")
    void testGetIndividualGroup_MissingAuthentication_ThrowException() {
        // When & Then - unauthenticated request is rejected before service call
        assertThrows(RuntimeException.class, () -> individualGroupController.getIndividualGroup(null));
        verify(individualGroupService, never()).getStudentGroupsCached("1001");
    }

    @Test
    @DisplayName("applyJoinGroup - authenticated user id is used")
    void testApplyJoinGroup_AuthenticatedUser_CallServiceWithPrincipal() {
        // Given - authenticated student and group id
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When - applying to join a group
        Result result = individualGroupController.applyJoinGroup(7, authentication);

        // Then - service receives principal from authentication
        assertTrue(result.getSuccess());
        verify(individualGroupService).applyJoinGroupAndNotify(7, "1001");
    }

    @Test
    @DisplayName("acceptJoin - manager endpoint delegates request parameters")
    void testAcceptJoin_ManagerEndpoint_CallService() {
        // When - manager accepts a student join request
        Result result = individualGroupController.acceptJoin(7, "1001");

        // Then - service receives request parameters
        assertTrue(result.getSuccess());
        verify(individualGroupService).acceptJoinAndNotify(7, "1001");
    }

    @Test
    @DisplayName("updatePermission - manager endpoint delegates request parameters")
    void testUpdatePermission_ManagerEndpoint_CallService() {
        // When - manager updates group permission
        Result result = individualGroupController.updatePermission(7, "1001", 2);

        // Then - service receives request parameters
        assertTrue(result.getSuccess());
        verify(individualGroupService).updatePermissionAndNotify(7, "1001", 2);
    }

    @Test
    @DisplayName("管理社团列表 - 使用请求中的管理员编号查询")
    void testGetAllManagedGroups_RequestManagerId_CallService() {
        // Given - manager id and empty managed group list
        when(individualGroupService.getAllManagedGroupsCached("manager001"))
                .thenReturn(Collections.emptyList());

        // When - manager queries managed groups
        Result result = individualGroupController.getAllManagedGroups("manager001", 1, 10);

        // Then - service receives requested manager id
        assertTrue(result.getSuccess());
        verify(individualGroupService).getAllManagedGroupsCached("manager001");
    }
}
