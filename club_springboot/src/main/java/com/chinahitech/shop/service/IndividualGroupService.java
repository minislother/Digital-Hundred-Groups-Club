package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;

import java.util.List;
import java.util.Map;

public interface IndividualGroupService {
    // 学生端：查询、申请加入社团。
    List<IndividualGroup> getGroupByStuId(String studentId);

    Map<String, Object> getStudentGroupsCached(String studentId);

    void applyJoinGroup(int groupId, String studentId);

    void applyJoinGroupAndNotify(int groupId, String studentId);

    // 管理端：成员列表、申请审批、成员维护和权限转让。
    List<IndividualGroup> getStudentsByGroup(int groupId, String searchInfo);

    List<IndividualGroup> getStudentsByGroupCached(int groupId, String searchInfo);

    List<IndividualGroup> getGroupApplyList(int groupId);

    List<IndividualGroup> getGroupApplyListCached(int groupId);

    void acceptJoin(int groupId, String studentId);

    void acceptJoinAndNotify(int groupId, String studentId);

    void acceptJoinAndNotify(int groupId, String studentId, String managerId);

    void rejectJoin(int groupId, String studentId);

    void rejectJoinAndNotify(int groupId, String studentId);

    void rejectJoinAndNotify(int groupId, String studentId, String managerId);

    void addGroupStudent(int groupId, String studentId, String position);

    void addGroupStudentAndNotify(int groupId, String studentId, String position);

    void addGroupStudentAndNotify(int groupId, String studentId, String position, String managerId);

    void modifyGroupStudent(int groupId, String studentId, String position);

    void modifyGroupStudentAndNotify(int groupId, String studentId, String position);

    void modifyGroupStudentAndNotify(int groupId, String studentId, String position, String managerId);

    void deleteGroupStudent(int groupId, String studentId);

    void deleteGroupStudentAndNotify(int groupId, String studentId);

    void deleteGroupStudentAndNotify(int groupId, String studentId, String managerId);

    List<Group> getAllManagedGroups(String managerId);

    List<Group> getAllManagedGroupsCached(String managerId);

    void transferStatus(int groupId, String managerId, String userId);

    void transferStatusAndNotify(int groupId, String managerId, String userId);

    void updatePermission(int groupId, String studentId, int status);

    void updatePermissionAndNotify(int groupId, String studentId, int status);

    void updatePermissionAndNotify(int groupId, String studentId, int status, String managerId);

    // 超级管理员端：全量成员查询和统计。
    List<IndividualGroup> getAllStudents(String searchInfo);

    List<GroupNum> getGroupMembers();

    List<GroupNum> getGroupMembersCached();
}
