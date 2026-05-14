package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.User;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TopManagerMapper extends BaseMapper<User> {

    User getTopManagerByNum(@Param("num") String num);

    User getUserByNum(@Param("num") String num);

    List<User> getAllUsers();

    List<User> getUser(@Param("userName") String userName);

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
                         @Param("status") int status,
                         @Param("modifyTime") Date modifyTime);

    User getByNumNoStatus(@Param("num") String num);
}
