package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.exception.UseridDuplicateException;
import com.chinahitech.shop.mapper.ManagerMapper;
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

@DisplayName("社团管理员账号服务单元测试")
@ExtendWith(MockitoExtension.class)
class ManagerServiceImplTest {

    @Mock
    private ManagerMapper managerMapper;

    @InjectMocks
    private ManagerServiceImpl managerService;

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("管理员登录 - 密码正确时返回管理员Token")
    void testLoginToken_ValidPassword_ReturnManagerToken() {
        // Given - 准备已启用的管理员账号和 BCrypt 密码。
        when(managerMapper.getByNum("clubadmin"))
                .thenReturn(user("clubadmin", PasswordUtil.encode("secret"), 1));

        // When - 执行登录并生成 JWT。
        String token = managerService.loginToken("clubadmin", "secret");

        // Then - Token 中应带有管理员身份，后续接口才能正确鉴权。
        Claims claims = JwtUtils.getClaimsByToken(token);
        assertEquals("clubadmin", claims.getSubject());
        assertEquals(JwtUtils.ROLE_MANAGER, claims.get("role"));
    }

    @Test
    @DisplayName("管理员登录 - 密码错误时返回空Token")
    void testLoginToken_WrongPassword_ReturnNull() {
        // Given - 数据库中保存的是正确密码的哈希。
        when(managerMapper.getByNum("clubadmin"))
                .thenReturn(user("clubadmin", PasswordUtil.encode("secret"), 1));

        // When - 使用错误密码登录。
        String token = managerService.loginToken("clubadmin", "bad-password");

        // Then - 不签发 Token，避免错误密码进入业务系统。
        assertNull(token);
    }

    @Test
    @DisplayName("管理员登录 - 账号未启用时返回空Token")
    void testLoginToken_DisabledAccount_ReturnNull() {
        // Given - 管理员账号存在但状态未启用。
        when(managerMapper.getByNum("clubadmin"))
                .thenReturn(user("clubadmin", PasswordUtil.encode("secret"), 0));

        // When - 使用正确密码登录。
        String token = managerService.loginToken("clubadmin", "secret");

        // Then - 未启用账号不能获得访问令牌。
        assertNull(token);
    }

    @Test
    @DisplayName("查询管理员 - 账号不存在时抛出异常")
    void testGetByUserId_MissingUser_ThrowException() {
        // Given - Mapper 未查询到账号。
        when(managerMapper.getByNum("missing")).thenReturn(null);

        // When & Then - 服务层统一抛出不存在异常。
        assertThrows(EntityNotFoundException.class, () -> managerService.getByUserId("missing"));
    }

    @Test
    @DisplayName("新增管理员 - 账号已存在时拒绝创建")
    void testAddManager_DuplicateUser_ThrowException() {
        // Given - 新账号已经被占用。
        when(managerMapper.getByNum("clubadmin")).thenReturn(user("clubadmin", "hash", 1));

        // When & Then - 直接抛出重复账号异常，不再写入数据库。
        assertThrows(UseridDuplicateException.class,
                () -> managerService.addManager("clubadmin", "secret", "club@example.com"));
        verify(managerMapper, never()).addManager(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(1));
    }

    @Test
    @DisplayName("新增管理员 - 信息合法时写入哈希密码")
    void testAddManager_Success_InsertHashedPassword() {
        // Given - 账号未被占用，Mapper 插入成功。
        when(managerMapper.getByNum("clubadmin")).thenReturn(null);
        when(managerMapper.addManager(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(1)))
                .thenReturn(1);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        // When - 新增管理员账号。
        managerService.addManager("clubadmin", "secret", "club@example.com");

        // Then - 保存到数据库的密码必须是哈希值，不能是明文。
        verify(managerMapper).addManager(eq("clubadmin"), passwordCaptor.capture(), eq("club@example.com"),
                anyString(), any(), any(), eq(1));
        assertNotEquals("secret", passwordCaptor.getValue());
        assertTrue(PasswordUtil.matches(passwordCaptor.getValue(), "secret"));
    }

    @Test
    @DisplayName("新增管理员 - 数据库插入失败时抛出异常")
    void testAddManager_InsertFailed_ThrowException() {
        // Given - 账号未占用但数据库写入失败。
        when(managerMapper.getByNum("clubadmin")).thenReturn(null);
        when(managerMapper.addManager(anyString(), anyString(), anyString(), anyString(), any(), any(), eq(1)))
                .thenReturn(0);

        // When & Then - 失败应显式暴露，避免前端误以为创建成功。
        assertThrows(InsertException.class,
                () -> managerService.addManager("clubadmin", "secret", "club@example.com"));
    }

    @Test
    @DisplayName("修改管理员密码 - 成功时重新哈希密码")
    void testUpdatePassword_Success_UpdateHashedPassword() {
        // Given - 准备现有管理员账号。
        when(managerMapper.getByNum("clubadmin")).thenReturn(user("clubadmin", PasswordUtil.encode("old"), 1));
        when(managerMapper.updateById(any(User.class))).thenReturn(1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When - 更新密码。
        managerService.updatePassword("clubadmin", "new-secret");

        // Then - 新密码以哈希形式保存，并刷新修改时间。
        verify(managerMapper).updateById(userCaptor.capture());
        assertTrue(PasswordUtil.matches(userCaptor.getValue().getPassword(), "new-secret"));
        assertNotNull(userCaptor.getValue().getModifyTime());
    }

    @Test
    @DisplayName("修改管理员手机号 - 更新失败时抛出异常")
    void testUpdatePhone_UpdateFailed_ThrowException() {
        // Given - 用户存在但数据库更新返回失败。
        when(managerMapper.getByNum("clubadmin")).thenReturn(user("clubadmin", "hash", 1));
        when(managerMapper.updateById(any(User.class))).thenReturn(0);

        // When & Then - 服务层抛出更新异常。
        assertThrows(UpdateException.class, () -> managerService.updatePhone("clubadmin", "13800138000"));
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
