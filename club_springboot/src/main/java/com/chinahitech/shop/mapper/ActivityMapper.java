package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Activity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ActivityMapper {

    // Student side
    List<Activity> query(@Param("searchInfo") String searchInfo);

    Activity getActivityByNameAndGroupName(@Param("activityName") String activityName,
                                           @Param("groupName") String groupName);

    List<Activity> queryTop();

    void applyJoin(@Param("activityId") Integer activityId,
                   @Param("studentId") Integer studentId,
                   @Param("status") Integer status);

    Map<String, Object> checkApply(@Param("activityId") Integer activityId,
                                   @Param("studentId") Integer studentId);

    int countActiveApplications(@Param("activityId") Integer activityId);

    List<Activity> getMyJoinedActivities(@Param("studentId") Integer studentId);

    // Manager side
    List<Activity> getActivityByGroupName(@Param("groupName") String groupName);

    List<Map<String, Object>> getActivityApplicants(@Param("activityId") Integer activityId);

    void auditApply(@Param("id") Integer id, @Param("status") Integer status);

    void insert(Activity activity);

    void updateDescription(@Param("groupName") String groupName,
                           @Param("activityName") String activityName,
                           @Param("description") String description,
                           @Param("attachment") String attachment,
                           @Param("image") String image);

    void updateAttachment(@Param("name") String name, @Param("attachment") String attachment);

    void updateImage(@Param("name") String name, @Param("image") String image);

    Activity getActivityById(@Param("id") Integer id);

    // Top manager side
    List<Activity> getAllApp(@Param("searchInfo") String searchInfo);

    Activity getAppByName(@Param("groupName") String groupName);

    void confirmApplication(@Param("activityId") Integer activityId);

    void denyApplication(@Param("activityId") Integer activityId);

    void modifyInfo(Activity activity);
}
