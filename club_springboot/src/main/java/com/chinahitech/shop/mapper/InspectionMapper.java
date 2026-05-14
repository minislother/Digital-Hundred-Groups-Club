package com.chinahitech.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinahitech.shop.bean.Inspection;
import org.apache.ibatis.annotations.Param;

import java.time.Year;
import java.util.Date;
import java.util.List;

public interface InspectionMapper extends BaseMapper<Inspection> {

    List<Inspection> findAll();

    List<Inspection> getBySearchInfo(@Param("searchInfo") String searchInfo);

    List<Inspection> getByGroup(@Param("groupName") String groupName);

    List<Inspection> getByGroupAndYear(@Param("groupName") String groupName,
                                       @Param("year") Year year);

    List<Inspection> getByYear(@Param("year") Year year);

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
}
