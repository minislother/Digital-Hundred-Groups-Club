package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IndividualGroupMapper {

    // 学生端：查询学生社团关系、检查重复申请、写入入团申请。
    List<IndividualGroup> getGroupByStuId(@Param("studentId") String studentId);

    IndividualGroup checkExist(@Param("studentId") String studentId,
                               @Param("groupId") int groupId);

    void insert(IndividualGroup individualGroup);

    // 管理端：成员维护、审批和当前管理员负责的社团范围。
    List<IndividualGroup> getStudentsByGroup(@Param("groupId") int groupId,
                                             @Param("searchInfo") String searchInfo);

    List<IndividualGroup> getGroupApplyList(@Param("groupId") int groupId);

    int updateStatus(@Param("groupId") int groupId,
                     @Param("studentId") String studentId,
                     @Param("status") int status);

    int updatePosition(@Param("groupId") int groupId,
                       @Param("studentId") String studentId,
                       @Param("position") String position);

    int delete(@Param("groupId") int groupId,
               @Param("studentId") String studentId);

    List<Group> getAllManagedGroups(@Param("managerId") String managerId);

    int countManagedGroup(@Param("groupId") int groupId,
                          @Param("managerId") String managerId);

    // 超级管理员端：全量成员查询和社团人数统计。
    List<IndividualGroup> getAllStudents(@Param("searchInfo") String searchInfo);

    List<GroupNum> getGroupMembers();
}
