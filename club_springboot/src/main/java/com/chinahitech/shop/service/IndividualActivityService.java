package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;

import java.util.List;

public interface IndividualActivityService {
    List<IndividualActivity> getActivityByStuId(String studentId);

    List<IndividualActivity> getActivityByStuIdCached(String studentId);

    void joinActivity(String studentId, int activityId);

    void joinActivityAndNotify(int activityId, String studentId);

    List<IndividualActivity> getActivityByActivityId(int activityId);

    List<IndividualActivity> getActivityByActivityIdCached(int activityId);

    List<IndividualActivity> getApplyByActivityId(int activityId);

    List<IndividualActivity> getApplyByActivityIdCached(int activityId);

    void addActivityStudent(String studentId, int activityId, String position, boolean approved);

    void addActivityStudentAndNotify(int activityId, String studentId, String position);

    void modifyActivityStudent(int activityId, String studentId, String position);

    void modifyActivityStudentAndNotify(int activityId, String studentId, String position);

    void deleteActivityStudent(int activityId, String studentId);

    void deleteActivityStudentAndNotify(int activityId, String studentId);

    List<Activity> getAllManagedActivities(String managerId);

    List<Activity> getAllManagedActivitiesCached(String managerId);

    void confirmApplication(int activityId, String studentId);

    void confirmApplicationAndNotify(int activityId, String studentId);

    void denyApplication(int activityId, String studentId);

    void denyApplicationAndNotify(int activityId, String studentId);

    List<ActivityNum> getActivityMembers();

    List<ActivityNum> getActivityMembersCached();
}
