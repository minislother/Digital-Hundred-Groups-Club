package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;

import java.util.List;

/**
 * 活动成员业务服务，负责学生报名、管理员审核维护活动成员和活动人数统计。
 */
public interface IndividualActivityService {
    // ==================== 学生端 ====================

    /**
     * 查询学生参与或申请中的活动关系记录。
     */
    List<IndividualActivity> getActivityByStuId(String studentId);

    /**
     * 从缓存优先读取学生参与或申请中的活动关系记录。
     */
    List<IndividualActivity> getActivityByStuIdCached(String studentId);

    /**
     * 为学生提交活动报名申请。
     */
    void joinActivity(String studentId, int activityId);

    /**
     * 为学生提交活动报名申请，并发送报名通知。
     */
    void joinActivityAndNotify(int activityId, String studentId);

    // ==================== 管理员端 ====================

    /**
     * 查询指定活动的已通过成员列表。
     */
    List<IndividualActivity> getActivityByActivityId(int activityId);

    /**
     * 从缓存优先读取指定活动的已通过成员列表。
     */
    List<IndividualActivity> getActivityByActivityIdCached(int activityId);

    /**
     * 查询指定活动的待审核报名列表。
     */
    List<IndividualActivity> getApplyByActivityId(int activityId);

    /**
     * 从缓存优先读取指定活动的待审核报名列表。
     */
    List<IndividualActivity> getApplyByActivityIdCached(int activityId);

    /**
     * 直接添加学生为活动成员。
     */
    void addActivityStudent(String studentId, int activityId, String position, boolean approved);

    /**
     * 直接添加学生为活动成员，并发送通知。
     */
    void addActivityStudentAndNotify(int activityId, String studentId, String position);

    /**
     * 以指定管理员身份直接添加学生为活动成员，并校验管理员权限后发送通知。
     */
    void addActivityStudentAndNotify(int activityId, String studentId, String position, String managerId);

    /**
     * 修改活动成员的职位或角色。
     */
    void modifyActivityStudent(int activityId, String studentId, String position);

    /**
     * 修改活动成员职位或角色，并发送通知。
     */
    void modifyActivityStudentAndNotify(int activityId, String studentId, String position);

    /**
     * 以指定管理员身份修改活动成员职位或角色，并校验管理员权限后发送通知。
     */
    void modifyActivityStudentAndNotify(int activityId, String studentId, String position, String managerId);

    /**
     * 从活动成员列表中移除学生。
     */
    void deleteActivityStudent(int activityId, String studentId);

    /**
     * 从活动成员列表中移除学生，并发送通知。
     */
    void deleteActivityStudentAndNotify(int activityId, String studentId);

    /**
     * 以指定管理员身份移除活动成员，并校验管理员权限后发送通知。
     */
    void deleteActivityStudentAndNotify(int activityId, String studentId, String managerId);

    /**
     * 查询指定管理员有权管理的活动列表。
     */
    List<Activity> getAllManagedActivities(String managerId);

    /**
     * 从缓存优先读取指定管理员有权管理的活动列表。
     */
    List<Activity> getAllManagedActivitiesCached(String managerId);

    /**
     * 通过学生的活动报名申请。
     */
    void confirmApplication(int activityId, String studentId);

    /**
     * 通过学生的活动报名申请，并发送通知。
     */
    void confirmApplicationAndNotify(int activityId, String studentId);

    /**
     * 以指定管理员身份通过活动报名申请，并校验管理员权限后发送通知。
     */
    void confirmApplicationAndNotify(int activityId, String studentId, String managerId);

    /**
     * 驳回学生的活动报名申请。
     */
    void denyApplication(int activityId, String studentId);

    /**
     * 驳回学生的活动报名申请，并发送通知。
     */
    void denyApplicationAndNotify(int activityId, String studentId);

    /**
     * 以指定管理员身份驳回活动报名申请，并校验管理员权限后发送通知。
     */
    void denyApplicationAndNotify(int activityId, String studentId, String managerId);

    // ==================== 超级管理员端 ====================

    /**
     * 统计各活动的成员数量。
     */
    List<ActivityNum> getActivityMembers();

    /**
     * 从缓存优先读取各活动成员数量统计。
     */
    List<ActivityNum> getActivityMembersCached();
}
