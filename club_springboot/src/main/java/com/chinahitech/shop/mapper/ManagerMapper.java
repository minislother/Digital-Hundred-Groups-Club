package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.User;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface ManagerMapper extends BaseMapper<User> {

    User getByNum(@Param("num") String num);

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
}
