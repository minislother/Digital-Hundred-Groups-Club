package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.Inspection;
import org.apache.ibatis.annotations.Param;

import java.time.Year;
import java.util.Date;
import java.util.List;

public interface InspectionMapper extends BaseMapper<Inspection> {

    List<Inspection> findAll();

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Inspection> getBySearchInfo(@Param("searchInfo") String searchInfo);

    /**
     * 读取并返回业务数据或页面状态。
     */
    List<Inspection> getByGroup(@Param("groupName") String groupName);

    List<Inspection> getByGroupAndYear(@Param("groupName") String groupName,
                                       /**
                                        * 读取并返回业务数据或页面状态。
                                        */
                                       @Param("year") Year year);

    List<Inspection> getByYear(@Param("year") Year year);

    /**
     * 读取并返回业务数据或页面状态。
     */
    Inspection getById(@Param("id") int id);

    int addFeedback(@Param("inspectionId") int inspectionId,
                    @Param("groupName") String groupName,
                    @Param("feedback") String feedback,
                    @Param("modifyTime") Date modifyTime);

    int modifyAttachment(@Param("inspectionId") int inspectionId,
                         @Param("groupName") String groupName,
                         @Param("attachment") String attachment,
                         @Param("modifyTime") Date modifyTime);

    int confirmApplicationByid(@Param("inspectionId") int inspectionId);

    int denyApplicationByid(@Param("inspectionId") int inspectionId);

    java.util.List<Inspection> getByGroupPage(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getByGroupCount(@org.apache.ibatis.annotations.Param("groupName") String groupName);

    java.util.List<Inspection> getByGroupAndYearPage(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("year") java.time.Year year, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getByGroupAndYearCount(@org.apache.ibatis.annotations.Param("groupName") String groupName, @org.apache.ibatis.annotations.Param("year") java.time.Year year);

    java.util.List<Inspection> getByYearPage(@org.apache.ibatis.annotations.Param("year") java.time.Year year, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getByYearCount(@org.apache.ibatis.annotations.Param("year") java.time.Year year);

    java.util.List<Inspection> getBySearchInfoPage(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo, @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("limit") int limit);
    long getBySearchInfoCount(@org.apache.ibatis.annotations.Param("searchInfo") String searchInfo);
}
