package com.chinahitech.shop.controller;

import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.TopManagerService;
import com.chinahitech.shop.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("TopManagerController unit tests")
@ExtendWith(MockitoExtension.class)
class TopManagerControllerTest {

    @Mock
    private TopManagerService topManagerService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private com.chinahitech.shop.service.UserImportService userImportService;

    @InjectMocks
    private TopManagerController topManagerController;

    @Test
    @DisplayName("上传文件 - 创建本人任务且不调用旧消息队列链路")
    void testUploadExcel_CreatesOwnedTask() {
        // Given - 已认证超级管理员上传文件。
        var file = new org.springframework.mock.web.MockMultipartFile("file", "users.xlsx", "application/octet-stream", new byte[]{1});
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin001", null);
        var task = new com.chinahitech.shop.bean.UserImportTask();
        task.setId("task-1");
        org.mockito.Mockito.when(userImportService.submit(file, "admin001")).thenReturn(task);
        // When - 接收上传。
        Result result = topManagerController.uploadExcel(file, authentication);
        // Then - 返回任务，旧同步及 Kafka 导入均未调用。
        assertTrue(result.getSuccess());
        assertEquals(task, result.getData().get("task"));
        verify(userImportService).submit(file, "admin001");
        org.mockito.Mockito.verifyNoInteractions(topManagerService, fileStorageService);
    }

    @Test
    @DisplayName("register - delegates to service")
    void testRegister_ValidUser_CallService() {
        // Given - a registration request
        RegisterUser user = new RegisterUser();
        user.setUserId("admin001");

        // When - registering a top manager
        Result result = topManagerController.register(user);

        // Then - service is invoked and success is returned
        assertTrue(result.getSuccess());
        verify(topManagerService).register(user);
    }

    @Test
    @DisplayName("modifyPassword - same user updates password")
    void testModifyPassword_SameUser_CallService() {
        // Given - authenticated top manager updates own password
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin001", null);

        // When - modifying password
        Result result = topManagerController.modifyPassword("admin001", "new-password", authentication);

        // Then - service is invoked
        assertTrue(result.getSuccess());
        verify(topManagerService).updatePassword("admin001", "new-password");
    }

    @Test
    @DisplayName("modifyPassword - different user is rejected")
    void testModifyPassword_DifferentUser_ThrowException() {
        // Given - authenticated top manager tries to modify another account through self-service API
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin001", null);

        // When & Then - cross-user operation is rejected
        assertThrows(BusinessException.class,
                () -> topManagerController.modifyPassword("admin002", "new-password", authentication));
        verify(topManagerService, never()).updatePassword("admin002", "new-password");
    }

    @Test
    @DisplayName("downloadExcel - non Excel file returns bad request")
    void testDownloadExcel_NonExcelFile_ReturnBadRequest() throws Exception {
        // Given - a non-excel file name
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When - downloading an invalid file type
        topManagerController.downloadExcel("users.txt", response);

        // Then - request is rejected before storage service access
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        verify(fileStorageService, never()).readUploadFile("users.txt");
    }
}
