package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Group;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {

    // Student side
    List<Group> query(@Param("searchInfo") String searchInfo);

    Group getByName(@Param("groupName") String groupName);

    List<Group> queryTop();

    void insert(Group group);

    void addHot(@Param("groupName") String groupName);

    // Manager side
    void updateDescription(@Param("groupName") String groupName,
                           @Param("description") String description,
                           @Param("attachment") String attachment,
                           @Param("image") String image);

    void updateAttachment(@Param("name") String name, @Param("attachment") String attachment);

    void updateImage(@Param("name") String name, @Param("image") String image);

    Group getGroupById(@Param("id") Integer id);

    // Top manager side
    List<Group> getAllApp(@Param("searchInfo") String searchInfo);

    Group getAppByName(@Param("groupName") String groupName);

    void confirmApplication(@Param("groupId") Integer groupId);

    void denyApplication(@Param("groupId") Integer groupId);
}
