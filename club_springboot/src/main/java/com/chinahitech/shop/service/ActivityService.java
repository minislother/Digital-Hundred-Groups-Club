package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;

import java.util.List;
import java.util.Map;

/**
 * 社团活动业务服务，负责学生端活动浏览/报名、管理员端活动维护和超级管理员端活动审核。
 */
public interface ActivityService {

    // ==================== 学生端 ====================

    /**
     * 按搜索条件查询可展示给学生的活动列表。
     */
    List<Activity> query(String searchInfo);

    /**
     * 从缓存优先读取学生端活动列表，缓存缺失时回源查询。
     */
    List<Activity> queryCached(String searchInfo);

    /**
     * 根据活动名称和所属社团名称查询活动详情。
     */
    Activity getActivityByNameAndGroupName(String activityName, String groupName);

    /**
     * 从缓存优先读取指定活动详情，缓存缺失时回源查询。
     */
    Activity getCachedActivityDetail(String activityName, String groupName);

    /**
     * 查询首页推荐或热门活动列表。
     */
    List<Activity> queryTop();

    /**
     * 从缓存优先读取首页推荐或热门活动列表。
     */
    List<Activity> queryTopCached();

    /**
     * 为学生提交活动报名申请。
     */
    void applyJoin(Integer activityId, Integer studentId);

    /**
     * 查询学生已经加入或报名成功的活动。
     */
    List<Activity> getMyJoinedActivities(Integer studentId);

    // ==================== 管理员端 ====================

    /**
     * 查询指定社团创建或管理的活动列表。
     */
    List<Activity> getActivityByGroupName(String groupName);

    /**
     * 查询指定活动的报名申请人列表。
     */
    List<Map<String, Object>> getActivityApplicants(Integer activityId);

    /**
     * 审核学生的活动报名申请。
     */
    void auditApply(Integer id, Integer status);

    /**
     * 创建新的社团活动并进入后续展示或审核流程。
     */
    void insert(Activity activity);

    /**
     * 更新活动说明、附件和封面图片等展示信息。
     */
    void updateDescription(String groupName, String activityName, String description, String attachment, String image);

    /**
     * 更新活动附件地址。
     */
    void updateAttachment(String name, String attachment);

    /**
     * 更新活动封面图片地址。
     */
    void updateImage(String name, String image);

    /**
     * 根据活动 ID 查询活动详情。
     */
    Activity getActivityById(Integer id);

    /**
     * 获取指定活动的附件地址。
     */
    String getAttachment(Integer id);

    // ==================== 超级管理员端 ====================

    /**
     * 查询超级管理员可审核的活动申请列表。
     */
    List<Activity> getAllApp(String searchinfo);

    /**
     * 根据社团名称查询待审核或已提交的活动申请详情。
     */
    Activity getAppByName(String groupname);

    /**
     * 通过活动创建或变更申请。
     */
    void confirmApplication(Integer activityId);

    /**
     * 驳回活动创建或变更申请。
     */
    void denyApplication(Integer activityId);

    /**
     * 修改活动的基础信息。
     */
    void modifyInfo(Activity activity);
}
