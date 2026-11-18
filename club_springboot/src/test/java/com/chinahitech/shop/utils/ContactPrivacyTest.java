package com.chinahitech.shop.utils;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.controller.TopManagerController;
import com.chinahitech.shop.service.TopManagerService;
import com.chinahitech.shop.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("个人信息展示权限测试")
class ContactPrivacyTest {
 @Test @DisplayName("列表脱敏 - 不修改原始用户且移除密码")
 void testSummary_Copy_DoesNotMutate() {
   // Given
   User user = new User(); user.setEmail("student@example.com"); user.setPhone("13800138000"); user.setPassword("hash");
   // When
   User copy = ContactPrivacy.summary(user);
   // Then
   assertEquals("138****8000",copy.getPhone()); assertEquals("s***@example.com",copy.getEmail());
   assertNull(copy.getPassword()); assertEquals("13800138000",user.getPhone());
 }
 @Test @DisplayName("完整联系方式 - 普通学生拒绝且不访问数据")
 void testDetail_Student_Deny() {
   // Given
   var service = mock(TopManagerService.class); var controller = new TopManagerController();
   ReflectionTestUtils.setField(controller,"topManagerService",service);
   var auth = new UsernamePasswordAuthenticationToken("s1","",List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
   // When & Then
   assertThrows(BusinessException.class, () -> controller.contactDetail("s2",auth));
   verifyNoInteractions(service);
 }
 @Test @DisplayName("完整联系方式 - 超级管理员按用户编号查询")
 void testDetail_TopManager_Allow() {
   // Given
   var service = mock(TopManagerService.class); var controller = new TopManagerController();
   ReflectionTestUtils.setField(controller,"topManagerService",service);
   User user = new User(); user.setEmail("student@example.com"); when(service.getContactUser("s1")).thenReturn(user);
   var auth = new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_TOP_MANAGER")));
   // When
   var result = controller.contactDetail("s1",auth);
   // Then
   assertNotNull(result); verify(service).getContactUser("s1");
 }
}
