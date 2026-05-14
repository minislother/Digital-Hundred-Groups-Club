package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;
import com.chinahitech.shop.service.IndividualActivityService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/individualActivity")
public class IndividualActivityController {

    @Autowired
    private IndividualActivityService individualActivityService;

    @RepeatLimit
    @RequestMapping("/allActivities")
    public Result getIndividualActivity(String studentId, Integer pageNum, Integer pageSize) {
        List<IndividualActivity> list = individualActivityService.getActivityByStuIdCached(studentId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/joinActivity")
    public Result joinActivity(int activityId, String studentId) {
        individualActivityService.joinActivityAndNotify(activityId, studentId);
        return Result.ok().message("申请提交成功，请等待管理员审核");
    }

    @RepeatLimit
    @RequestMapping("/getAllStudents")
    public Result getStudentsByActivity(int activityId, Integer pageNum, Integer pageSize) {
        List<IndividualActivity> list = individualActivityService.getActivityByActivityIdCached(activityId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/getAllApplyStudents")
    public Result getAllApplyStudents(int activityId, Integer pageNum, Integer pageSize) {
        List<IndividualActivity> list = individualActivityService.getApplyByActivityIdCached(activityId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/addActivityStudent")
    public Result addActivityStudent(int activityId, String studentId, String position) {
        individualActivityService.addActivityStudentAndNotify(activityId, studentId, position);
        return Result.ok().message("添加成功");
    }

    @RepeatLimit
    @RequestMapping("/modifyGroupStudent")
    public Result modifyGroupStudent(int activityId, String studentId, String position) {
        individualActivityService.modifyActivityStudentAndNotify(activityId, studentId, position);
        return Result.ok().message("修改成功");
    }

    @RepeatLimit
    @RequestMapping("/deleteActivityStudent")
    public Result deleteActivityStudent(int activityId, String studentId) {
        individualActivityService.deleteActivityStudentAndNotify(activityId, studentId);
        return Result.ok().message("删除成功");
    }

    @RepeatLimit
    @RequestMapping("/allManagedActivities")
    public Result getAllManagedActivities(String managerId, Integer pageNum, Integer pageSize) {
        List<Activity> activityList = individualActivityService.getAllManagedActivitiesCached(managerId);
        return PageUtils.ok("items", activityList, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/accept")
    public Result acceptApplication(int activityId, String studentId) {
        individualActivityService.confirmApplicationAndNotify(activityId, studentId);
        return Result.ok().message("审批通过");
    }

    @RepeatLimit
    @PostMapping("/reject")
    public Result rejectApplication(int activityId, String studentId) {
        individualActivityService.denyApplicationAndNotify(activityId, studentId);
        return Result.ok().message("已拒绝");
    }

    @RepeatLimit
    @RequestMapping("/getActivityMembers")
    public Result getActivityMembers() {
        List<ActivityNum> list = individualActivityService.getActivityMembersCached();
        return Result.ok().data("items", list);
    }
}
