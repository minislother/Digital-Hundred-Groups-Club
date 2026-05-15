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

@DisplayName("个人社团控制器单元测试")
@ExtendWith(MockitoExtension.class)
class IndividualGroupControllerTest {

    @Mock
    private IndividualGroupService individualGroupService;

    @InjectMocks
    private IndividualGroupController individualGroupController;

    @Test
    @DisplayName("学生社团列表 - 使用认证学生编号")
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
    @DisplayName("学生社团列表 - 缺少认证抛出异常")
    void testGetIndividualGroup_MissingAuthentication_ThrowException() {
        // When & Then - unauthenticated request is rejected before service call
        assertThrows(RuntimeException.class, () -> individualGroupController.getIndividualGroup(null));
        verify(individualGroupService, never()).getStudentGroupsCached("1001");
    }

    @Test
    @DisplayName("申请加入社团 - 使用认证学生编号")
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
    @DisplayName("批准入团申请 - 使用认证管理员编号")
    void testAcceptJoin_ManagerEndpoint_CallServiceWithAuthenticatedManager() {
        // Given - authenticated manager
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);

        // When - manager accepts a student join request
        Result result = individualGroupController.acceptJoin(7, "1001", authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualGroupService).acceptJoinAndNotify(7, "1001", "manager001");
    }

    @Test
    @DisplayName("修改成员权限 - 使用认证管理员编号")
    void testUpdatePermission_ManagerEndpoint_CallServiceWithAuthenticatedManager() {
        // Given - authenticated manager
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);

        // When - manager updates group permission
        Result result = individualGroupController.updatePermission(7, "1001", 2, authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualGroupService).updatePermissionAndNotify(7, "1001", 2, "manager001");
    }

    @Test
    @DisplayName("管理社团列表 - 使用认证管理员编号")
    void testGetAllManagedGroups_AuthenticatedManagerId_CallService() {
        // Given - authenticated manager and empty managed group list
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);
        when(individualGroupService.getAllManagedGroupsCached("manager001"))
                .thenReturn(Collections.emptyList());

        // When - manager queries managed groups
        Result result = individualGroupController.getAllManagedGroups(1, 10, authentication);

        // Then - service receives authenticated manager id
        assertTrue(result.getSuccess());
        verify(individualGroupService).getAllManagedGroupsCached("manager001");
    }

    @Test
    @DisplayName("转让社团权限 - 使用认证管理员作为原管理员")
    void testTransferStatus_ManagerEndpoint_CallServiceWithAuthenticatedManager() {
        // Given - authenticated manager
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager001", null);

        // When - manager transfers group ownership
        Result result = individualGroupController.transferStatus(7, "manager002", authentication);

        // Then - service receives authenticated manager id as source manager
        assertTrue(result.getSuccess());
        verify(individualGroupService).transferStatusAndNotify(7, "manager001", "manager002");
    }
}
