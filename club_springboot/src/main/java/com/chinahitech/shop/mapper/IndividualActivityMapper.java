package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IndividualActivityMapper {

    // Student side
    List<IndividualActivity> getActivityByStuId(@Param("studentId") String studentId);

    IndividualActivity checkExist(@Param("studentId") String studentId,
                                  @Param("activityId") int activityId);

    void insert(IndividualActivity individualActivity);

    // Manager side
    List<IndividualActivity> getActivityByActivityId(@Param("activityId") int activityId);

    List<IndividualActivity> getApplyByActivityId(@Param("activityId") int activityId);

    void updatePosition(@Param("activityId") int activityId,
                        @Param("studentId") String studentId,
                        @Param("position") String position);

    void delete(@Param("activityId") int activityId,
                @Param("studentId") String studentId);

    List<Activity> getAllManagedActivities(@Param("managerId") String managerId);

    void updateStatus(@Param("activityId") int activityId,
                      @Param("studentId") String studentId,
                      @Param("status") int status);

    // Top manager side
    List<ActivityNum> getActivityMembers();
}
