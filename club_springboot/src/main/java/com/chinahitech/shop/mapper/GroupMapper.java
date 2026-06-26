package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Group;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {

    // Student side
    List<Group> query(@Param("searchInfo") String searchInfo);

    /**
     * 读取并返回业务数据或页面状态。
     */
    Group getByName(@Param("groupName") String groupName);

    List<Group> queryTop();

    void insert(Group group);

    /**
     * 新增业务数据或追加界面状态。
     */
    void addHot(@Param("groupName") String groupName);

    // Manager side
    void updateDescription(@Param("groupName") String groupName,
                           /**
                            * 更新表单字段或业务数据。
                            */
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
    Group getGroupById(@Param("id") Integer id);

    // Top manager side
    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Group> getAllApp(@Param("searchInfo") String searchInfo);

    /**
     * 读取并返回业务数据或页面状态。
     */
    Group getAppByName(@Param("groupName") String groupName);

    void confirmApplication(@Param("groupId") Integer groupId);

    void denyApplication(@Param("groupId") Integer groupId);
}
