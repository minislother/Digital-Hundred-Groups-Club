package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Group;
import java.util.List;
import java.util.Map;

public interface GroupService {

    // ==================== 学生端 ====================
    List<Group> query(String searchInfo);
    List<Group> queryCached(String searchInfo);
    Group getByName(String groupName);
    Group getCachedGroupDetail(String groupName);
    List<Group> queryTop();
    List<Group> queryTopCached();
    void insert(Group group);
    void addHot(String groupName);

    // ==================== 管理员端 ====================
    void updateDescription(String groupname, String description, String attachment, String image);
    void updateAttachment(String name, String attachment);
    void updateImage(String name, String image);
    Group getGroupById(Integer id);
    String getAttachment(Integer id);

    // ==================== 超级管理员端 ====================
    List<Group> getAllApp(String searchinfo);
    Group getAppByName(String groupname);
    void confirmApplication(Integer groupId);
    void denyApplication(Integer groupId);
}
