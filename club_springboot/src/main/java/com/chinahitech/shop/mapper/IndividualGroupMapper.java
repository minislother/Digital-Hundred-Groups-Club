package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IndividualGroupMapper {

    // Student side
    List<IndividualGroup> getGroupByStuId(@Param("studentId") String studentId);

    IndividualGroup checkExist(@Param("studentId") String studentId,
                               @Param("groupId") int groupId);

    void insert(IndividualGroup individualGroup);

    // Manager side
    List<IndividualGroup> getStudentsByGroup(@Param("groupId") int groupId,
                                             @Param("searchInfo") String searchInfo);

    List<IndividualGroup> getGroupApplyList(@Param("groupId") int groupId);

    void updateStatus(@Param("groupId") int groupId,
                      @Param("studentId") String studentId,
                      @Param("status") int status);

    void updatePosition(@Param("groupId") int groupId,
                        @Param("studentId") String studentId,
                        @Param("position") String position);

    void delete(@Param("groupId") int groupId,
                @Param("studentId") String studentId);

    List<Group> getAllManagedGroups(@Param("managerId") String managerId);

    // Top manager side
    List<IndividualGroup> getAllStudents(@Param("searchInfo") String searchInfo);

    List<GroupNum> getGroupMembers();
}
