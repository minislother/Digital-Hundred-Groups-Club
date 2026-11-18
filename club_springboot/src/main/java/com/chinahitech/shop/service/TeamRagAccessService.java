package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.config.TeamRagProperties;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.GroupMapper;
import com.chinahitech.shop.mapper.IndividualGroupMapper;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/** 在数据库当前权限下授权，退出社团后不能继续使用旧会话访问知识。 */
@Service
@RequiredArgsConstructor
public class TeamRagAccessService {
    private final TeamRagProperties properties;
    private final GroupMapper groups;
    private final IndividualGroupMapper members;

    public java.util.List<java.util.Map<String, Object>> availableGroups(Authentication authentication) {
        if (!properties.isEnabled()) throw new BusinessException("SERVICE_UNAVAILABLE", "知识库尚未启用");
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (int id : new java.util.TreeSet<>(properties.getProjects().keySet())) {
            try {
                authorize(id, authentication, false);
                var group = groups.getGroupById(id);
                if (group != null) result.add(java.util.Map.of("id", id, "name", group.getName()));
            } catch (BusinessException denied) {
                // 只展示当前用户有权访问且配置完整的知识库，不返回其他社团的项目映射。
            }
        }
        return result;
    }

    public Access authorize(int groupId, Authentication authentication, boolean write) {
        String userId = SecurityUtils.currentUserId(authentication);
        if (!properties.isEnabled()) throw new BusinessException("SERVICE_UNAVAILABLE", "知识库尚未启用");
        if (groupId <= 0 || groups.getGroupById(groupId) == null)
            throw new BusinessException("NOT_FOUND", "社团不存在");
        boolean top = hasRole(authentication, "TOP_MANAGER");
        boolean manager = hasRole(authentication, "MANAGER") && members.countManagedGroup(groupId, userId) > 0;
        if (!top && !manager) {
            IndividualGroup member = hasRole(authentication, "STUDENT") ? members.checkExist(userId, groupId) : null;
            if (write || member == null || member.getStatus() != 1)
                throw new BusinessException("FORBIDDEN", "无权访问该社团知识库");
        }
        String project = properties.getProjects().get(groupId);
        if (project == null || project.isBlank() || project.equals("*"))
            throw new BusinessException("SERVICE_UNAVAILABLE", "该社团尚未配置知识库");
        // 共享项目只提供通用资料，管理员也不能向该项目上传社团私有文档。
        if (write && properties.getReadOnlyProjects().contains(project))
            throw new BusinessException("FORBIDDEN", "该知识库为共享只读库");
        String role = top ? "TOP_MANAGER" : manager ? "MANAGER" : "STUDENT";
        return new Access(project, "baituan-" + role + "-" + userId, write ? "EDITOR" : "READER");
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public record Access(String project, String owner, String role) { }
}
