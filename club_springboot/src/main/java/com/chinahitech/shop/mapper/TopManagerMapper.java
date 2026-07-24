package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.User;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TopManagerMapper extends BaseMapper<User> {

    /**
     * 读取并返回业务数据或页面状态。
     */
    User getTopManagerByNum(@Param("num") String num);

    /**
     * 读取并返回业务数据或页面状态。
     */
    User getUserByNum(@Param("num") String num);

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<User> getAllUsers();

    /** 只读取当前页用户，避免超级管理员列表把十万级用户全部加载到 JVM。 */
    List<User> getAllUsersPage(@Param("offset") int offset, @Param("limit") int limit);

    /** 统计平台用户总数。 */
    long countAllUsers();

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<User> getUser(@Param("userName") String userName);

    /** 按用户名筛选并分页。 */
    List<User> getUserPage(@Param("userName") String userName,
                           @Param("offset") int offset,
                           @Param("limit") int limit);

    /** 统计符合用户名筛选条件的用户数。 */
    long countUser(@Param("userName") String userName);

    int addManager(@Param("userId") String userId,
                   @Param("password") String password,
                   @Param("email") String email,
                   @Param("salt") String salt,
                   @Param("createTime") Date createTime,
                   @Param("modifyTime") Date modifyTime,
                   @Param("status") int status);

    int updatePassword(@Param("userId") String userId,
                       @Param("password") String password,
                       @Param("modifyTime") Date modifyTime);

    int updatePhone(@Param("userId") String userId,
                    @Param("phone") String phone,
                    @Param("modifyTime") Date modifyTime);

    int updateDescription(@Param("userId") String userId,
                          @Param("description") String description,
                          @Param("modifyTime") Date modifyTime);

    int updateNickname(@Param("userId") String userId,
                       @Param("nickname") String nickname,
                       @Param("modifyTime") Date modifyTime);

    int updateMajor(@Param("userId") String userId,
                    @Param("campus") String campus,
                    @Param("school") String school,
                    @Param("major") String major,
                    @Param("modifyTime") Date modifyTime);

    int updatePermission(@Param("userId") String userId,
                         /**
                          * 读取并返回业务数据或页面状态。
                          */
                         @Param("status") int status,
                         @Param("modifyTime") Date modifyTime);

    User getByNumNoStatus(@Param("num") String num);
}
