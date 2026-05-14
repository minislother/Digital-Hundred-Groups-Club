package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.User;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface StuMapper extends BaseMapper<User> {

    User getByNum(@Param("num") String num);

    User getByName(@Param("name") String name);

    int addStudent(@Param("stuNumber") String stuNumber,
                   @Param("password") String password,
                   @Param("email") String email,
                   @Param("salt") String salt,
                   @Param("createTime") Date createTime,
                   @Param("modifyTime") Date modifyTime,
                   @Param("status") int status);

    int updatePassword(@Param("stuNumber") String stuNumber,
                       @Param("password") String password,
                       @Param("modifyTime") Date modifyTime);

    int updatePhone(@Param("stuNumber") String stuNumber,
                    @Param("phone") String phone,
                    @Param("modifyTime") Date modifyTime);

    int updateDescription(@Param("stuNumber") String stuNumber,
                          @Param("description") String description,
                          @Param("modifyTime") Date modifyTime);

    int updateNickname(@Param("stuNumber") String stuNumber,
                       @Param("nickname") String nickname,
                       @Param("modifyTime") Date modifyTime);
}
