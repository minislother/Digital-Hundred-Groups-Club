package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Activity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ActivityMapper {

    // Student side
    List<Activity> query(@Param("searchInfo") String searchInfo);

    /** 只查询当前页活动，避免在 Java 内存中对全表结果切页。 */
    List<Activity> queryPage(@Param("searchInfo") String searchInfo,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    /** 统计符合学生端筛选条件的活动总数。 */
    long countQuery(@Param("searchInfo") String searchInfo);

    Activity getActivityByNameAndGroupName(@Param("activityName") String activityName,
                                           @Param("groupName") String groupName);

    List<Activity> queryTop();

    void applyJoin(@Param("activityId") Integer activityId,
                   @Param("studentId") Integer studentId,
                   @Param("status") Integer status);

    Map<String, Object> checkApply(@Param("activityId") Integer activityId,
                                   @Param("studentId") Integer studentId);

    int countActiveApplications(@Param("activityId") Integer activityId);

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Activity> getMyJoinedActivities(@Param("studentId") Integer studentId);

    // Manager side
    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Activity> getActivityByGroupName(@Param("groupName") String groupName);

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Map<String, Object>> getActivityApplicants(@Param("activityId") Integer activityId);

    void auditApply(@Param("id") Integer id, @Param("status") Integer status);

    void insert(Activity activity);

    void updateDescription(@Param("groupName") String groupName,
                           /**
                            * 更新表单字段或业务数据。
                            */
                           @Param("activityName") String activityName,
                           @Param("description") String description,
                           @Param("attachment") String attachment,
                           @Param("image") String image);

    void updateAttachment(@Param("name") String name, @Param("attachment") String attachment);

    /**
     * 更新表单字段或业务数据。
     */
    void updateImage(@Param("name") String name, @Param("image") String image);

    /**
     * 读取并返回业务数据或页面状态。
     */
    Activity getActivityById(@Param("id") Integer id);

    // Top manager side
    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Activity> getAllApp(@Param("searchInfo") String searchInfo);

    /**
     * 读取并返回业务数据或页面状态。
     */
    Activity getAppByName(@Param("groupName") String groupName);

    void confirmApplication(@Param("activityId") Integer activityId);

    void denyApplication(@Param("activityId") Integer activityId);

    void modifyInfo(Activity activity);
}
