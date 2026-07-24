package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Group;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {

    // Student side
    List<Group> query(@Param("searchInfo") String searchInfo);

    /** 只查询当前页社团，防止分页请求把整张表加载到 JVM。 */
    List<Group> queryPage(@Param("searchInfo") String searchInfo,
                          @Param("offset") int offset,
                          @Param("limit") int limit);

    /** 统计符合学生端筛选条件的社团总数，用于生成分页元数据。 */
    long countQuery(@Param("searchInfo") String searchInfo);

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
