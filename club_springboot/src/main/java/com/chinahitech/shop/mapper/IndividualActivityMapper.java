package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IndividualActivityMapper {

    // 学生端：查询学生活动关系、检查重复报名、写入报名申请。
    List<IndividualActivity> getActivityByStuId(@Param("studentId") String studentId);

    IndividualActivity checkExist(@Param("studentId") String studentId,
                                  /**
                                   * 新增申请关系数据，将用户申请写入数据库。
                                   */
                                  @Param("activityId") int activityId);

    void insert(IndividualActivity individualActivity);

    // 管理端：活动成员维护、报名审批和当前管理员负责的活动范围。
    List<IndividualActivity> getActivityByActivityId(@Param("activityId") int activityId);

    /**
     * 按活动 ID 查询该活动的报名申请列表。
     */
    List<IndividualActivity> getApplyByActivityId(@Param("activityId") int activityId);

    int updatePosition(@Param("activityId") int activityId,
                       @Param("studentId") String studentId,
                       @Param("position") String position);

    int delete(@Param("activityId") int activityId,
               /**
                * 读取并返回业务数据或页面状态。
                */
               @Param("studentId") String studentId);

    List<Activity> getAllManagedActivities(@Param("managerId") String managerId);

    int countManagedActivity(@Param("activityId") int activityId,
                             @Param("managerId") String managerId);

    int updateStatus(@Param("activityId") int activityId,
                     @Param("studentId") String studentId,
                     @Param("status") int status);

    // 超级管理员端：活动人数统计。
    List<ActivityNum> getActivityMembers();
}
