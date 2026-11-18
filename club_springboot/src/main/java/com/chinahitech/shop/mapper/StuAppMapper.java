package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.StuApp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuAppMapper extends BaseMapper<StuApp> {

    List<StuApp> findall();

    List<StuApp> findMyapp(@Param("stunumber") String stunumber);

    List<StuApp> findRecvapp(@Param("groupName") String groupName);

    /**
     * 读取并返回业务数据或页面状态。
     */
    StuApp getById(@Param("id") Integer id);

    List<StuApp> getByStuAndGroup(@Param("studentId") String studentId,
                                  @Param("groupName") String groupName);

    String findIsAccepted(@Param("id") Integer id);

    int updateAttachment(@Param("applicationId") int applicationId,
                         @Param("attachment") String attachment);

    int confirmApplicationByid(@Param("applicationId") Integer applicationId);

    int denyApplicationByid(@Param("applicationId") Integer applicationId);

    java.util.List<StuApp> findallPage(@org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long findallCount();

    java.util.List<StuApp> findMyappPage(@org.apache.ibatis.annotations.Param("stunumber") String stunumber, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long findMyappCount(@org.apache.ibatis.annotations.Param("stunumber") String stunumber);

    java.util.List<StuApp> findRecvappPage(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long findRecvappCount(@org.apache.ibatis.annotations.Param("groupName") String groupName);
}
