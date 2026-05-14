package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.StuApp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuAppMapper extends BaseMapper<StuApp> {

    List<StuApp> findall();

    List<StuApp> findMyapp(@Param("stunumber") String stunumber);

    List<StuApp> findRecvapp(@Param("groupName") String groupName);

    StuApp getById(@Param("id") Integer id);

    List<StuApp> getByStuAndGroup(@Param("studentId") String studentId,
                                  @Param("groupName") String groupName);

    String findIsAccepted(@Param("id") Integer id);

    int updateAttachment(@Param("applicationId") int applicationId,
                         @Param("attachment") String attachment);

    int confirmApplicationByid(@Param("applicationId") Integer applicationId);

    int denyApplicationByid(@Param("applicationId") Integer applicationId);
}
