package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;

import java.util.List;

/**
 * 社团业务服务，负责社团查询、详情展示、资料维护和超级管理员审核。
 */
public interface GroupService {

    // ==================== 学生端 ====================

    /**
     * 按搜索条件查询学生端可浏览的社团列表。
     */
    List<Group> query(String searchInfo);

    /**
     * 从缓存优先读取学生端社团列表，缓存缺失时回源查询。
     */
    List<Group> queryCached(String searchInfo);

    /**
     * 根据社团名称查询社团详情。
     */
    Group getByName(String groupName);

    /**
     * 从缓存优先读取社团详情，缓存缺失时回源查询。
     */
    Group getCachedGroupDetail(String groupName);

    /**
     * 查询首页推荐或热门社团列表。
     */
    List<Group> queryTop();

    /**
     * 从缓存优先读取首页推荐或热门社团列表。
     */
    List<Group> queryTopCached();

    /**
     * 新增社团申请或社团记录。
     */
    void insert(Group group);

    /**
     * 增加社团热度，用于推荐或排序。
     */
    void addHot(String groupName);

    // ==================== 管理员端 ====================

    /**
     * 更新社团介绍、附件和封面图片等展示信息。
     */
    void updateDescription(String groupname, String description, String attachment, String image);

    /**
     * 更新社团附件地址。
     */
    void updateAttachment(String name, String attachment);

    /**
     * 更新社团封面图片地址。
     */
    void updateImage(String name, String image);

    /**
     * 根据社团 ID 查询社团详情。
     */
    Group getGroupById(Integer id);

    /**
     * 获取指定社团的附件地址。
     */
    String getAttachment(Integer id);

    // ==================== 超级管理员端 ====================

    /**
     * 查询超级管理员可审核的社团申请列表。
     */
    List<Group> getAllApp(String searchinfo);

    /**
     * 根据社团名称查询社团申请详情。
     */
    Group getAppByName(String groupname);

    /**
     * 通过社团创建或变更申请。
     */
    void confirmApplication(Integer groupId);

    /**
     * 驳回社团创建或变更申请。
     */
    void denyApplication(Integer groupId);
}
