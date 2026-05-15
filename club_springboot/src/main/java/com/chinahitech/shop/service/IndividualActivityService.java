package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;

import java.util.List;

public interface IndividualActivityService {
    // 学生端：查询已参与活动、申请报名活动。
    List<IndividualActivity> getActivityByStuId(String studentId);

    List<IndividualActivity> getActivityByStuIdCached(String studentId);

    void joinActivity(String studentId, int activityId);

    void joinActivityAndNotify(int activityId, String studentId);

    // 管理端：活动成员查询、报名审批和成员维护。
    List<IndividualActivity> getActivityByActivityId(int activityId);

    List<IndividualActivity> getActivityByActivityIdCached(int activityId);

    List<IndividualActivity> getApplyByActivityId(int activityId);

    List<IndividualActivity> getApplyByActivityIdCached(int activityId);

    void addActivityStudent(String studentId, int activityId, String position, boolean approved);

    void addActivityStudentAndNotify(int activityId, String studentId, String position);

    void addActivityStudentAndNotify(int activityId, String studentId, String position, String managerId);

    void modifyActivityStudent(int activityId, String studentId, String position);

    void modifyActivityStudentAndNotify(int activityId, String studentId, String position);

    void modifyActivityStudentAndNotify(int activityId, String studentId, String position, String managerId);

    void deleteActivityStudent(int activityId, String studentId);

    void deleteActivityStudentAndNotify(int activityId, String studentId);

    void deleteActivityStudentAndNotify(int activityId, String studentId, String managerId);

    List<Activity> getAllManagedActivities(String managerId);

    List<Activity> getAllManagedActivitiesCached(String managerId);

    void confirmApplication(int activityId, String studentId);

    void confirmApplicationAndNotify(int activityId, String studentId);

    void confirmApplicationAndNotify(int activityId, String studentId, String managerId);

    void denyApplication(int activityId, String studentId);

    void denyApplicationAndNotify(int activityId, String studentId);

    void denyApplicationAndNotify(int activityId, String studentId, String managerId);

    // 超级管理员端：活动成员数统计。
    List<ActivityNum> getActivityMembers();

    List<ActivityNum> getActivityMembersCached();
}
