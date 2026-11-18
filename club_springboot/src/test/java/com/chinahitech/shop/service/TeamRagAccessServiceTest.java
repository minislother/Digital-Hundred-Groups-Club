package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.config.TeamRagProperties;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.GroupMapper;
import com.chinahitech.shop.mapper.IndividualGroupMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRag社团权限测试")
class TeamRagAccessServiceTest {
    @Mock GroupMapper groups;
    @Mock IndividualGroupMapper members;
    TeamRagProperties properties;
    TeamRagAccessService service;
    @BeforeEach void setup() {
        properties = new TeamRagProperties(); properties.setEnabled(true); properties.getProjects().put(1, "club-one");
        service = new TeamRagAccessService(properties, groups, members);
    }
    private UsernamePasswordAuthenticationToken user(String role) {
        return new UsernamePasswordAuthenticationToken("u1", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    @Test @DisplayName("知识库列表 - 不展示无权限社团")
    void testAvailableGroups_FilterOtherClubs() {
        // Given
        properties.getProjects().put(2,"club-two");
        Group first=new Group(); first.setName("摄影社");
        Group second=new Group(); second.setName("音乐社");
        when(groups.getGroupById(1)).thenReturn(first); when(groups.getGroupById(2)).thenReturn(second);
        IndividualGroup member=new IndividualGroup(); member.setStatus(1); when(members.checkExist("u1",1)).thenReturn(member);
        // When
        var available=service.availableGroups(user("STUDENT"));
        // Then
        assertEquals(1,available.size()); assertEquals("摄影社",available.getFirst().get("name"));
        assertFalse(available.getFirst().containsKey("project"));
    }
    @Test @DisplayName("已入团学生 - 可读且身份包含角色")
    void testAuthorize_Member_Read() {
        // Given
        when(groups.getGroupById(1)).thenReturn(new Group());
        IndividualGroup member = new IndividualGroup(); member.setStatus(1);
        when(members.checkExist("u1", 1)).thenReturn(member);
        // When
        var access = service.authorize(1, user("STUDENT"), false);
        // Then
        assertEquals("club-one", access.project()); assertEquals("baituan-STUDENT-u1", access.owner());
        assertEquals("READER", access.role());
    }
    @Test @DisplayName("待审批或离团学生 - 拒绝访问")
    void testAuthorize_NotMember_Reject() {
        // Given
        when(groups.getGroupById(1)).thenReturn(new Group());
        when(members.checkExist("u1", 1)).thenReturn(new IndividualGroup());
        // When / Then
        assertEquals("FORBIDDEN", assertThrows(BusinessException.class, () -> service.authorize(1,user("STUDENT"),false)).getCode());
    }
    @Test @DisplayName("学生上传资料 - 拒绝且不调用TeamRag")
    void testAuthorize_StudentWrite_Reject() {
        // Given
        when(groups.getGroupById(1)).thenReturn(new Group());
        // When / Then
        assertThrows(BusinessException.class, () -> service.authorize(1,user("STUDENT"),true));
    }
    @Test @DisplayName("社团管理员 - 仅能写入自己管理的社团")
    void testAuthorize_Manager_ScopedWrite() {
        // Given
        when(groups.getGroupById(1)).thenReturn(new Group()); when(members.countManagedGroup(1,"u1")).thenReturn(1,0);
        // When / Then
        assertEquals("EDITOR",service.authorize(1,user("MANAGER"),true).role());
        assertThrows(BusinessException.class, () -> service.authorize(1,user("MANAGER"),true));
    }
    @Test @DisplayName("超级管理员 - 仍禁止使用全项目通配映射")
    void testAuthorize_Wildcard_Reject() {
        // Given
        when(groups.getGroupById(1)).thenReturn(new Group()); properties.getProjects().put(1,"*");
        // When / Then
        assertThrows(BusinessException.class, () -> service.authorize(1,user("TOP_MANAGER"),false));
    }
    @Test @DisplayName("功能未启用 - 不访问数据库")
    void testAuthorize_Disabled_Reject() {
        // Given
        properties.setEnabled(false);
        // When / Then
        assertThrows(BusinessException.class, () -> service.authorize(1,user("STUDENT"),false)); verifyNoInteractions(groups,members);
    }
}
