package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.exception.UseridDuplicateException;
import com.chinahitech.shop.mapper.TopManagerMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PasswordUtil;
import com.chinahitech.shop.utils.PageResult;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TopManagerServiceImpl unit tests")
@ExtendWith(MockitoExtension.class)
class TopManagerServiceImplTest {

    @Mock
    private TopManagerMapper topManagerMapper;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private TopManagerServiceImpl topManagerService;

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("getAllUsersPage - database pagination returns current page metadata")
    void testGetAllUsersPage_NoSearch_ReturnCurrentPage() {
        User user = user("user001", "encoded", 0);
        when(topManagerMapper.getAllUsersPage(20, 10)).thenReturn(Collections.singletonList(user));
        when(topManagerMapper.countAllUsers()).thenReturn(100016L);

        PageResult<User> page = topManagerService.getAllUsersPage(null, 3, 10);

        assertEquals(1, page.getRecords().size());
        assertEquals(100016L, page.getTotal());
        assertEquals(3, page.getPageNum());
        verify(topManagerMapper).getAllUsersPage(20, 10);
    }

    @Test
    @DisplayName("loginToken - valid password returns top manager token")
    void testLoginToken_ValidPassword_ReturnToken() {
        when(topManagerMapper.getTopManagerByNum("admin001"))
                .thenReturn(user("admin001", PasswordUtil.encode("secret"), 10));

        String token = topManagerService.loginToken("admin001", "secret");

        Claims claims = JwtUtils.getClaimsByToken(token);
        assertEquals("admin001", claims.getSubject());
        assertEquals(JwtUtils.ROLE_TOP_MANAGER, claims.get("role"));
    }

    @Test
    @DisplayName("loginToken - wrong password returns null")
    void testLoginToken_WrongPassword_ReturnNull() {
        when(topManagerMapper.getTopManagerByNum("admin001"))
                .thenReturn(user("admin001", PasswordUtil.encode("secret"), 10));

        String token = topManagerService.loginToken("admin001", "bad-password");

        assertNull(token);
    }

    @Test
    @DisplayName("getByUserId - missing user throws exception")
    void testGetByUserId_MissingUser_ThrowException() {
        when(topManagerMapper.getTopManagerByNum("missing")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> topManagerService.getByUserId("missing"));
    }

    @Test
    @DisplayName("getAllUsers - blank search uses full list")
    void testGetAllUsers_BlankSearch_UseAllUsers() {
        User user = user("1001", "pwd", 0);
        when(topManagerMapper.getAllUsers()).thenReturn(Collections.singletonList(user));

        assertEquals(1, topManagerService.getAllUsers("  ").size());
        verify(topManagerMapper).getAllUsers();
        verify(topManagerMapper, never()).getUser(anyString());
    }

    @Test
    @DisplayName("getAllUsers - keyword search uses filtered query")
    void testGetAllUsers_WithSearch_UseFilteredQuery() {
        User user = user("1001", "pwd", 0);
        when(topManagerMapper.getUser("alice")).thenReturn(Collections.singletonList(user));

        assertEquals(1, topManagerService.getAllUsers("alice").size());
        verify(topManagerMapper).getUser("alice");
        verify(topManagerMapper, never()).getAllUsers();
    }

    @Test
    @DisplayName("addUser - hashes password and inserts inactive user")
    void testAddUser_Success_InsertHashedUser() {
        when(topManagerMapper.insert(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        topManagerService.addUser("1001", "plain-password", "user@example.com");

        verify(topManagerMapper).insert(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertEquals("1001", inserted.getUserId());
        assertEquals("user@example.com", inserted.getEmail());
        assertEquals(0, inserted.getStatus());
        assertNotEquals("plain-password", inserted.getPassword());
        assertTrue(PasswordUtil.matches(inserted.getPassword(), "plain-password"));
        assertNotNull(inserted.getSalt());
        assertNotNull(inserted.getCreateTime());
        assertNotNull(inserted.getModifyTime());
    }

    @Test
    @DisplayName("addUser - insert failure throws exception")
    void testAddUser_InsertFailed_ThrowException() {
        when(topManagerMapper.insert(any(User.class))).thenReturn(0);

        assertThrows(InsertException.class,
                () -> topManagerService.addUser("1001", "plain-password", "user@example.com"));
    }

    @Test
    @DisplayName("updatePhone - updates existing user")
    void testUpdatePhone_Success_UpdateUser() {
        when(topManagerMapper.getTopManagerByNum("1001")).thenReturn(user("1001", "pwd", 0));
        when(topManagerMapper.updateById(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        topManagerService.updatePhone("1001", "13800138000");

        verify(topManagerMapper).updateById(userCaptor.capture());
        assertEquals("13800138000", userCaptor.getValue().getPhone());
        assertNotNull(userCaptor.getValue().getModifyTime());
    }

    @Test
    @DisplayName("updateNickname - update failure throws exception")
    void testUpdateNickname_UpdateFailed_ThrowException() {
        when(topManagerMapper.getTopManagerByNum("1001")).thenReturn(user("1001", "pwd", 0));
        when(topManagerMapper.updateById(any(User.class))).thenReturn(0);

        assertThrows(UpdateException.class,
                () -> topManagerService.updateNickname("1001", "new-name"));
    }

    @Test
    @DisplayName("deleteUser - delete failure throws exception")
    void testDeleteUser_DeleteFailed_ThrowException() {
        User user = user("1001", "pwd", 0);
        when(topManagerMapper.deleteById("1001")).thenReturn(0);

        assertThrows(UpdateException.class, () -> topManagerService.deleteUser(user));
    }

    @Test
    @DisplayName("enqueueUploadExcel - kafka success skips local import")
    void testEnqueueUploadExcel_KafkaSuccess_SendOnly() {
        when(kafkaProducer.send(anyString(), anyString())).thenReturn(true);

        topManagerService.enqueueUploadExcel("D:/tmp/users.xlsx");

        verify(kafkaProducer).send(anyString(), org.mockito.ArgumentMatchers.contains("D:/tmp/users.xlsx"));
        verify(topManagerMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("addManager - duplicate manager id throws exception")
    void testAddManager_DuplicateUser_ThrowException() {
        // Given - manager id already exists
        when(topManagerMapper.getTopManagerByNum("manager001")).thenReturn(user("manager001", "pwd", 10));

        // When & Then - duplicate account is rejected before insert
        assertThrows(UseridDuplicateException.class,
                () -> topManagerService.addManager("manager001", "plain-password", "manager@example.com"));
        verify(topManagerMapper, never()).addManager(anyString(), anyString(), anyString(), anyString(), any(Date.class), any(Date.class), eq(10));
    }

    @Test
    @DisplayName("addManager - hashes password and inserts active manager")
    void testAddManager_Success_InsertHashedManager() {
        // Given - manager id is available
        when(topManagerMapper.getTopManagerByNum("manager001")).thenReturn(null);
        when(topManagerMapper.addManager(eq("manager001"), anyString(), eq("manager@example.com"), anyString(), any(Date.class), any(Date.class), eq(10)))
                .thenReturn(1);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        // When - creating manager account
        topManagerService.addManager("manager001", "plain-password", "manager@example.com");

        // Then - password is stored as hash and account status is active manager status
        verify(topManagerMapper).addManager(eq("manager001"), passwordCaptor.capture(), eq("manager@example.com"), anyString(), any(Date.class), any(Date.class), eq(10));
        assertNotEquals("plain-password", passwordCaptor.getValue());
        assertTrue(PasswordUtil.matches(passwordCaptor.getValue(), "plain-password"));
    }

    @Test
    @DisplayName("updateMajor - updates campus school and major")
    void testUpdateMajor_Success_UpdateUserEducationInfo() {
        // Given - target top manager exists
        when(topManagerMapper.getTopManagerByNum("1001")).thenReturn(user("1001", "pwd", 10));
        when(topManagerMapper.updateById(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When - updating education information
        topManagerService.updateMajor("1001", "南校区", "计算机学院", "软件工程");

        // Then - mapper receives the modified fields
        verify(topManagerMapper).updateById(userCaptor.capture());
        assertEquals("南校区", userCaptor.getValue().getCampus());
        assertEquals("计算机学院", userCaptor.getValue().getSchool());
        assertEquals("软件工程", userCaptor.getValue().getMajor());
        assertNotNull(userCaptor.getValue().getModifyTime());
    }

    @Test
    @DisplayName("updateUserInfo - update failure throws exception")
    void testUpdateUserInfo_UpdateFailed_ThrowException() {
        // Given - mapper reports no row updated
        User user = user("1001", "pwd", 0);
        when(topManagerMapper.updateById(user)).thenReturn(0);

        // When & Then - failed update is surfaced as service exception
        assertThrows(UpdateException.class, () -> topManagerService.updateUserInfo(user));
        assertNotNull(user.getModifyTime());
    }

    private User user(String userId, String password, int status) {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setStatus(status);
        return user;
    }
}
