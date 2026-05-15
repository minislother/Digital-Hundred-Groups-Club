package com.chinahitech.shop.controller;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.GroupService;
import com.chinahitech.shop.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GroupController unit tests")
@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private GroupController groupController;

    @Test
    @DisplayName("all - returns paged cached groups")
    void testGetAll_ReturnPagedGroups() {
        Group group = new Group();
        group.setName("computer");
        when(groupService.queryCached("com")).thenReturn(Collections.singletonList(group));

        Result result = groupController.getAll("com", 1, 10);

        assertTrue(result.getSuccess());
        assertSame(group, ((java.util.List<?>) result.getData().get("items")).get(0));
        verify(groupService).queryCached("com");
    }

    @Test
    @DisplayName("studentDetail - returns cached group detail")
    void testGetStudentDetail_ReturnGroup() {
        Group group = new Group();
        when(groupService.getCachedGroupDetail("computer")).thenReturn(group);

        Result result = groupController.getStudentDetail("computer");

        assertTrue(result.getSuccess());
        assertSame(group, result.getData().get("group"));
        verify(groupService).getCachedGroupDetail("computer");
    }

    @Test
    @DisplayName("addGroup - delegates insert")
    void testAddGroup_CallService() {
        Group group = new Group();

        Result result = groupController.addGroup(group);

        assertTrue(result.getSuccess());
        verify(groupService).insert(group);
    }

    @Test
    @DisplayName("managerDetail - returns cached group detail")
    void testGetManagerDetail_ReturnGroup() {
        Group group = new Group();
        when(groupService.getCachedGroupDetail("computer")).thenReturn(group);

        Result result = groupController.getManagerDetail("computer");

        assertTrue(result.getSuccess());
        assertSame(group, result.getData().get("group"));
        verify(groupService).getCachedGroupDetail("computer");
    }

    @Test
    @DisplayName("modifyDescription - forwards all fields")
    void testModifyDescription_CallService() {
        Result result = groupController.modifyDescription("computer", "desc", "club.zip", "cover.png");

        assertTrue(result.getSuccess());
        verify(groupService).updateDescription("computer", "desc", "club.zip", "cover.png");
    }

    @Test
    @DisplayName("uploadZip - returns stored file url")
    void testUploadFile_ReturnFileUrl() {
        when(fileStorageService.store(multipartFile))
                .thenReturn(new StoredFile("club.zip", "upload/club.zip", "/upload/club.zip"));

        ResponseEntity<Map<String, String>> response = groupController.uploadFile(multipartFile);

        assertEquals("/upload/club.zip", response.getBody().get("fileUrl"));
        verify(fileStorageService).store(multipartFile);
    }

    @Test
    @DisplayName("submitZip - updates attachment")
    void testSubmitZip_CallService() {
        ResponseEntity<Map<String, String>> response = groupController.submitZip("club.zip", "computer");

        assertEquals("Successfully updated attachment.", response.getBody().get("message"));
        verify(groupService).updateAttachment("computer", "club.zip");
    }

    @Test
    @DisplayName("submitPhoto - updates image")
    void testSubmitPhoto_CallService() {
        ResponseEntity<Map<String, String>> response = groupController.submitPhoto("cover.png", "computer");

        assertEquals("Successfully updated image.", response.getBody().get("message"));
        verify(groupService).updateImage("computer", "cover.png");
    }

    @Test
    @DisplayName("getAttachment - returns attachment")
    void testGetAttachment_ReturnAttachment() {
        when(groupService.getAttachment(7)).thenReturn("club.zip");

        ResponseEntity<Map<String, Object>> response = groupController.getAttachment(7);

        assertEquals(20000, response.getBody().get("code"));
        assertEquals("club.zip", response.getBody().get("attachment"));
        verify(groupService).getAttachment(7);
    }

    @Test
    @DisplayName("allApps - returns paged applications")
    void testGetAllApps_ReturnPagedApplications() {
        Group group = new Group();
        when(groupService.getAllApp("computer")).thenReturn(Collections.singletonList(group));

        Result result = groupController.getAllApps("computer", 1, 10);

        assertTrue(result.getSuccess());
        assertSame(group, ((java.util.List<?>) result.getData().get("items")).get(0));
        verify(groupService).getAllApp("computer");
    }

    @Test
    @DisplayName("appDetail - returns application detail")
    void testGetAppDetail_ReturnGroup() {
        Group group = new Group();
        when(groupService.getAppByName("computer")).thenReturn(group);

        Result result = groupController.getAppDetail("computer");

        assertTrue(result.getSuccess());
        assertSame(group, result.getData().get("group"));
        verify(groupService).getAppByName("computer");
    }

    @Test
    @DisplayName("accept - confirms application")
    void testAccept_CallService() {
        Result result = groupController.accept(7);

        assertTrue(result.getSuccess());
        verify(groupService).confirmApplication(7);
    }

    @Test
    @DisplayName("reject - denies application")
    void testReject_CallService() {
        Result result = groupController.reject(7);

        assertTrue(result.getSuccess());
        verify(groupService).denyApplication(7);
    }
}
