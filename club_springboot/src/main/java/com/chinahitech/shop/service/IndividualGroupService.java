package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;

import java.util.List;
import java.util.Map;

/**
 * 社团成员业务服务，负责入团申请、成员维护、管理员权限转移和社团人数统计。
 */
public interface IndividualGroupService {
    // ==================== 学生端 ====================

    /**
     * 查询学生加入或申请中的社团关系记录。
     */
    List<IndividualGroup> getGroupByStuId(String studentId);

    /**
     * 从缓存优先读取学生加入或申请中的社团信息。
     */
    Map<String, Object> getStudentGroupsCached(String studentId);

    /**
     * 为学生提交加入社团申请。
     */
    void applyJoinGroup(int groupId, String studentId);

    /**
     * 为学生提交加入社团申请，并发送通知。
     */
    void applyJoinGroupAndNotify(int groupId, String studentId);

    // ==================== 管理员端 ====================

    /**
     * 查询指定社团的成员列表，可按条件筛选。
     */
    List<IndividualGroup> getStudentsByGroup(int groupId, String searchInfo);

    /**
     * 从缓存优先读取指定社团的成员列表。
     */
    List<IndividualGroup> getStudentsByGroupCached(int groupId, String searchInfo);

    /**
     * 查询指定社团的待审核入团申请列表。
     */
    List<IndividualGroup> getGroupApplyList(int groupId);

    /**
     * 从缓存优先读取指定社团的待审核入团申请列表。
     */
    List<IndividualGroup> getGroupApplyListCached(int groupId);

    /**
     * 通过学生的入团申请。
     */
    void acceptJoin(int groupId, String studentId);

    /**
     * 通过学生的入团申请，并发送通知。
     */
    void acceptJoinAndNotify(int groupId, String studentId);

    /**
     * 以指定管理员身份通过入团申请，并校验管理员权限后发送通知。
     */
    void acceptJoinAndNotify(int groupId, String studentId, String managerId);

    /**
     * 驳回学生的入团申请。
     */
    void rejectJoin(int groupId, String studentId);

    /**
     * 驳回学生的入团申请，并发送通知。
     */
    void rejectJoinAndNotify(int groupId, String studentId);

    /**
     * 以指定管理员身份驳回入团申请，并校验管理员权限后发送通知。
     */
    void rejectJoinAndNotify(int groupId, String studentId, String managerId);

    /**
     * 直接添加学生为社团成员。
     */
    void addGroupStudent(int groupId, String studentId, String position);

    /**
     * 直接添加学生为社团成员，并发送通知。
     */
    void addGroupStudentAndNotify(int groupId, String studentId, String position);

    /**
     * 以指定管理员身份直接添加社团成员，并校验管理员权限后发送通知。
     */
    void addGroupStudentAndNotify(int groupId, String studentId, String position, String managerId);

    /**
     * 修改社团成员职位或角色。
     */
    void modifyGroupStudent(int groupId, String studentId, String position);

    /**
     * 修改社团成员职位或角色，并发送通知。
     */
    void modifyGroupStudentAndNotify(int groupId, String studentId, String position);

    /**
     * 以指定管理员身份修改社团成员职位或角色，并校验管理员权限后发送通知。
     */
    void modifyGroupStudentAndNotify(int groupId, String studentId, String position, String managerId);

    /**
     * 从社团成员列表中移除学生。
     */
    void deleteGroupStudent(int groupId, String studentId);

    /**
     * 从社团成员列表中移除学生，并发送通知。
     */
    void deleteGroupStudentAndNotify(int groupId, String studentId);

    /**
     * 以指定管理员身份移除社团成员，并校验管理员权限后发送通知。
     */
    void deleteGroupStudentAndNotify(int groupId, String studentId, String managerId);

    /**
     * 查询指定管理员有权管理的社团列表。
     */
    List<Group> getAllManagedGroups(String managerId);

    /**
     * 从缓存优先读取指定管理员有权管理的社团列表。
     */
    List<Group> getAllManagedGroupsCached(String managerId);

    /**
     * 将社团管理员身份转交给指定用户。
     */
    void transferStatus(int groupId, String managerId, String userId);

    /**
     * 将社团管理员身份转交给指定用户，并发送通知。
     */
    void transferStatusAndNotify(int groupId, String managerId, String userId);

    /**
     * 更新社团成员权限状态。
     */
    void updatePermission(int groupId, String studentId, int status);

    /**
     * 更新社团成员权限状态，并发送通知。
     */
    void updatePermissionAndNotify(int groupId, String studentId, int status);

    /**
     * 以指定管理员身份更新社团成员权限，并校验管理员权限后发送通知。
     */
    void updatePermissionAndNotify(int groupId, String studentId, int status, String managerId);

    // ==================== 超级管理员端 ====================

    /**
     * 查询全站社团成员关系，可按条件筛选。
     */
    List<IndividualGroup> getAllStudents(String searchInfo);

    /**
     * 统计各社团的成员数量。
     */
    List<GroupNum> getGroupMembers();

    /**
     * 从缓存优先读取各社团成员数量统计。
     */
    List<GroupNum> getGroupMembersCached();
}
