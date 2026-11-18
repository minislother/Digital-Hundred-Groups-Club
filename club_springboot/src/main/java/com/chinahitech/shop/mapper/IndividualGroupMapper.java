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
                               /**
                                * 新增申请关系数据，将用户申请写入数据库。
                                */
                               @Param("groupId") int groupId);

    void insert(IndividualGroup individualGroup);

    /** 成员和申请使用同一组筛选条件完成数据库分页与计数。 */
    List<IndividualGroup> getMembershipPage(@Param("groupId") int groupId,
            @Param("status") int status, @Param("searchInfo") String searchInfo,
            @Param("offset") int offset, @Param("limit") int limit);
    long countMembership(@Param("groupId") int groupId,
            @Param("status") int status, @Param("searchInfo") String searchInfo);

    // 管理端：成员维护、审批和当前管理员负责的社团范围。
    List<IndividualGroup> getStudentsByGroup(@Param("groupId") int groupId,
                                             /**
                                              * 查询指定社团的入团申请列表。
                                              */
                                             @Param("searchInfo") String searchInfo);

    List<IndividualGroup> getGroupApplyList(@Param("groupId") int groupId);

    int updateStatus(@Param("groupId") int groupId,
                     @Param("studentId") String studentId,
                     @Param("status") int status);

    int updatePosition(@Param("groupId") int groupId,
                       @Param("studentId") String studentId,
                       @Param("position") String position);

    int delete(@Param("groupId") int groupId,
               /**
                * 读取并返回业务数据或页面状态。
                */
               @Param("studentId") String studentId);

    List<Group> getAllManagedGroups(@Param("managerId") String managerId);

    /** 按页查询管理员负责的社团。 */
    List<Group> getAllManagedGroupsPage(@Param("managerId") String managerId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 统计管理员负责的社团总数。 */
    long countAllManagedGroups(@Param("managerId") String managerId);

    int countManagedGroup(@Param("groupId") int groupId,
                          @Param("managerId") String managerId);

    // 超级管理员端：全量成员查询和社团人数统计。
    List<IndividualGroup> getAllStudents(@Param("searchInfo") String searchInfo);

    /**
     * 查询指定社团的成员列表。
     */
    List<GroupNum> getGroupMembers();

    java.util.List<IndividualGroup> getAllStudentsPage(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getAllStudentsCount(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo);

    java.util.List<IndividualGroup> studentSlice(@org.apache.ibatis.annotations.Param("studentId") String studentId,
            @org.apache.ibatis.annotations.Param("beforeId") Long beforeId,
            @org.apache.ibatis.annotations.Param("limit") int limit);

    java.util.List<IndividualGroup> studentPage(@org.apache.ibatis.annotations.Param("studentId") String studentId,
            @org.apache.ibatis.annotations.Param("offset") int offset, @org.apache.ibatis.annotations.Param("limit") int limit);
    long countStudent(@org.apache.ibatis.annotations.Param("studentId") String studentId);

    java.util.List<IndividualGroup> memberSlice(@org.apache.ibatis.annotations.Param("resourceId") int resourceId,
            @org.apache.ibatis.annotations.Param("status") int status,
            @org.apache.ibatis.annotations.Param("beforeId") Long beforeId,
            @org.apache.ibatis.annotations.Param("limit") int limit);
}
