package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;

import java.util.List;
import java.util.Map;

public interface IndividualGroupService {
    List<IndividualGroup> getGroupByStuId(String studentId);

    Map<String, Object> getStudentGroupsCached(String studentId);

    void applyJoinGroup(int groupId, String studentId);

    void applyJoinGroupAndNotify(int groupId, String studentId);

    List<IndividualGroup> getStudentsByGroup(int groupId, String searchInfo);

    List<IndividualGroup> getStudentsByGroupCached(int groupId, String searchInfo);

    List<IndividualGroup> getGroupApplyList(int groupId);

    List<IndividualGroup> getGroupApplyListCached(int groupId);

    void acceptJoin(int groupId, String studentId);

    void acceptJoinAndNotify(int groupId, String studentId);

    void rejectJoin(int groupId, String studentId);

    void rejectJoinAndNotify(int groupId, String studentId);

    void addGroupStudent(int groupId, String studentId, String position);

    void addGroupStudentAndNotify(int groupId, String studentId, String position);

    void modifyGroupStudent(int groupId, String studentId, String position);

    void modifyGroupStudentAndNotify(int groupId, String studentId, String position);

    void deleteGroupStudent(int groupId, String studentId);

    void deleteGroupStudentAndNotify(int groupId, String studentId);

    List<Group> getAllManagedGroups(String managerId);

    List<Group> getAllManagedGroupsCached(String managerId);

    void transferStatus(int groupId, String managerId, String userId);

    void transferStatusAndNotify(int groupId, String managerId, String userId);

    void updatePermission(int groupId, String studentId, int status);

    void updatePermissionAndNotify(int groupId, String studentId, int status);

    List<IndividualGroup> getAllStudents(String searchInfo);

    List<GroupNum> getGroupMembers();

    List<GroupNum> getGroupMembersCached();
}
