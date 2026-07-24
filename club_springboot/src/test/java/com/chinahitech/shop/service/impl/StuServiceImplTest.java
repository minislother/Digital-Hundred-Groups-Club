package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.exception.UseridDuplicateException;
import com.chinahitech.shop.mapper.StuMapper;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PasswordUtil;
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

@DisplayName("学生账号服务单元测试")
@ExtendWith(MockitoExtension.class)
class StuServiceImplTest {

    @Mock
    private StuMapper stuMapper;

    @InjectMocks
    private StuServiceImpl stuService;

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("学生登录 - 密码正确时返回学生Token")
    void testLoginToken_ValidPassword_ReturnStudentToken() {
        // Given - 准备学生账号和 BCrypt 密码。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", PasswordUtil.encode("secret"), 0));

        // When - 执行学生登录。
        String token = stuService.loginToken("20240001", "secret");

        // Then - Token 中应标识学生角色。
        Claims claims = JwtUtils.getClaimsByToken(token);
        assertEquals("20240001", claims.getSubject());
        assertEquals(JwtUtils.ROLE_STUDENT, claims.get("role"));
    }

    @Test
    @DisplayName("学生登录 - 密码错误时返回空Token")
    void testLoginToken_WrongPassword_ReturnNull() {
        // Given - 数据库中保存的是正确密码哈希。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", PasswordUtil.encode("secret"), 0));

        // When - 使用错误密码登录。
        String token = stuService.loginToken("20240001", "bad-password");

        // Then - 不签发 Token。
        assertNull(token);
    }

    @Test
    @DisplayName("查询学生 - 学号不存在时抛出异常")
    void testGetByStuNumber_MissingStudent_ThrowException() {
        // Given - Mapper 查询不到学生。
        when(stuMapper.getByNum("missing")).thenReturn(null);

        // When & Then - 服务层统一抛出不存在异常。
        assertThrows(EntityNotFoundException.class, () -> stuService.getByStuNumber("missing"));
    }

    @Test
    @DisplayName("新增学生 - 学号已存在时拒绝创建")
    void testAddStudent_DuplicateUser_ThrowException() {
        // Given - 学号已被注册。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", "hash", 0));

        // When & Then - 不再写入数据库。
        assertThrows(UseridDuplicateException.class,
                () -> stuService.addStudent("20240001", "secret", "student@example.com"));
        verify(stuMapper, never()).addStudent(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(0));
    }

    @Test
    @DisplayName("新增学生 - 信息合法时写入哈希密码")
    void testAddStudent_Success_InsertHashedPassword() {
        // Given - 学号未注册，Mapper 插入成功。
        when(stuMapper.getByNum("20240001")).thenReturn(null);
        when(stuMapper.addStudent(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(0)))
                .thenReturn(1);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        // When - 新增学生账号。
        stuService.addStudent("20240001", "secret", "student@example.com");

        // Then - 保存的密码必须为哈希值。
        verify(stuMapper).addStudent(eq("20240001"), passwordCaptor.capture(), eq("student@example.com"),
                anyString(), any(), any(), eq(0));
        assertNotEquals("secret", passwordCaptor.getValue());
        assertTrue(PasswordUtil.matches(passwordCaptor.getValue(), "secret"));
    }

    @Test
    @DisplayName("新增学生 - 数据库插入失败时抛出异常")
    void testAddStudent_InsertFailed_ThrowException() {
        // Given - 学号未占用但数据库写入失败。
        when(stuMapper.getByNum("20240001")).thenReturn(null);
        when(stuMapper.addStudent(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(0)))
                .thenReturn(0);

        // When & Then - 创建失败要显式抛出异常。
        assertThrows(InsertException.class,
                () -> stuService.addStudent("20240001", "secret", "student@example.com"));
    }

    @Test
    @DisplayName("修改学生密码 - 成功时重新哈希密码")
    void testUpdatePassword_Success_UpdateHashedPassword() {
        // Given - 准备已有学生账号。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", PasswordUtil.encode("old"), 0));
        when(stuMapper.updateById(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When - 更新学生密码。
        stuService.updatePassword("20240001", "new-secret");

        // Then - 新密码不能以明文保存。
        verify(stuMapper).updateById(userCaptor.capture());
        assertTrue(PasswordUtil.matches(userCaptor.getValue().getPassword(), "new-secret"));
        assertNotNull(userCaptor.getValue().getModifyTime());
    }

    @Test
    @DisplayName("修改学生简介 - 更新成功时写入新简介")
    void testUpdateDescription_Success_UpdateDescription() {
        // Given - 学生存在且数据库更新成功。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", "hash", 0));
        when(stuMapper.updateById(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When - 更新个人简介。
        stuService.updateDescription("20240001", "热爱社团活动");

        // Then - 简介和修改时间都会被写入。
        verify(stuMapper).updateById(userCaptor.capture());
        assertEquals("热爱社团活动", userCaptor.getValue().getDescription());
        assertNotNull(userCaptor.getValue().getModifyTime());
    }

    @Test
    @DisplayName("修改学生昵称 - 更新失败时抛出异常")
    void testUpdateNickname_UpdateFailed_ThrowException() {
        // Given - 用户存在但数据库更新失败。
        when(stuMapper.getByNum("20240001")).thenReturn(user("20240001", "hash", 0));
        when(stuMapper.updateById(any(User.class))).thenReturn(0);

        // When & Then - 服务层抛出更新异常。
        assertThrows(UpdateException.class, () -> stuService.updateNickname("20240001", "新昵称"));
    }

    private User user(String userId, String password, int status) {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setStatus(status);
        user.setSalt("salt");
        return user;
    }
}
