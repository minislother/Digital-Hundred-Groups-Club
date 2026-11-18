package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Activity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ActivityMapper {
    /** 搜索只从 ES 取得排名；主库复核可见性并返回完整业务字段，防止审核撤回后泄露旧文档。 */
    @org.apache.ibatis.annotations.Select({"<script>SELECT * FROM activity WHERE is_accepted=1 AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>"})
    List<Activity> searchVisibleByIds(@Param("ids") List<Integer> ids);

    /** 所有名额变更共用的行锁，必须在事务内调用。 */
    Activity getActivityForUpdate(@Param("id") int id);

    com.chinahitech.shop.bean.IndividualActivity getApplicationById(@Param("id") int id);

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

    java.util.List<Activity> getActivityByGroupNamePage(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("searchInfo") String searchInfo, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getActivityByGroupNameCount(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("searchInfo") String searchInfo);

    java.util.List<Activity> getAllAppPage(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getAllAppCount(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo);

    java.util.List<Activity> getMyJoinedActivitiesPage(@org.apache.ibatis.annotations.Param("studentId") int studentId, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getMyJoinedActivitiesCount(@org.apache.ibatis.annotations.Param("studentId") int studentId);

    java.util.List<java.util.Map<String, Object>> getActivityApplicantsPage(@org.apache.ibatis.annotations.Param("activityId") int activityId, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getActivityApplicantsCount(@org.apache.ibatis.annotations.Param("activityId") int activityId);

    java.util.List<Activity> searchSlice(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo,
            @org.apache.ibatis.annotations.Param("beforeId") Long beforeId,
            @org.apache.ibatis.annotations.Param("limit") int limit);
}
