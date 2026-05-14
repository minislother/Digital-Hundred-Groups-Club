package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;

import java.util.List;
import java.util.Map;

public interface ActivityService {

    // ==================== 学生端 ====================
    List<Activity> query(String searchInfo);

    List<Activity> queryCached(String searchInfo);

    Activity getActivityByNameAndGroupName(String activityName, String groupName);

    Activity getCachedActivityDetail(String activityName, String groupName);

    List<Activity> queryTop();

    List<Activity> queryTopCached();

    void applyJoin(Integer activityId, Integer studentId);

    List<Activity> getMyJoinedActivities(Integer studentId);

    // ==================== 管理员端 ====================
    List<Activity> getActivityByGroupName(String groupName);

    List<Map<String, Object>> getActivityApplicants(Integer activityId);

    void auditApply(Integer id, Integer status);

    void insert(Activity activity);

    void updateDescription(String groupName, String activityName, String description, String attachment, String image);

    void updateAttachment(String name, String attachment);

    void updateImage(String name, String image);

    Activity getActivityById(Integer id);

    String getAttachment(Integer id);

    // ==================== 超级管理员 ====================
    List<Activity> getAllApp(String searchinfo);

    Activity getAppByName(String groupname);

    void confirmApplication(Integer activityId);

    void denyApplication(Integer activityId);

    void modifyInfo(Activity activity);
}
