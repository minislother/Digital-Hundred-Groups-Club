package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;
import com.chinahitech.shop.service.IndividualActivityService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人活动关系接口。
 *
 * <p>学生报名使用认证学生编号；管理端成员维护和审批使用认证管理员编号，
 * 由 Service 层校验该管理员是否拥有目标活动的管理权限。</p>
 */
@RestController
@RequestMapping("/individualActivity")
public class IndividualActivityController {

    @Autowired
    private IndividualActivityService individualActivityService;

    @RepeatLimit
    @RequestMapping("/allActivities")
    public Result getIndividualActivity(Integer pageNum, Integer pageSize, Authentication authentication) {
        String studentId = SecurityUtils.currentUserId(authentication);
        List<IndividualActivity> list = individualActivityService.getActivityByStuIdCached(studentId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/joinActivity")
    public Result joinActivity(int activityId, Authentication authentication) {
        String studentId = SecurityUtils.currentUserId(authentication);
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
    public Result addActivityStudent(int activityId, String studentId, String position, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualActivityService.addActivityStudentAndNotify(activityId, studentId, position, managerId);
        return Result.ok().message("添加成功");
    }

    @RepeatLimit
    @RequestMapping("/modifyGroupStudent")
    public Result modifyGroupStudent(int activityId, String studentId, String position, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualActivityService.modifyActivityStudentAndNotify(activityId, studentId, position, managerId);
        return Result.ok().message("修改成功");
    }

    @RepeatLimit
    @RequestMapping("/deleteActivityStudent")
    public Result deleteActivityStudent(int activityId, String studentId, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualActivityService.deleteActivityStudentAndNotify(activityId, studentId, managerId);
        return Result.ok().message("删除成功");
    }

    @RepeatLimit
    @RequestMapping("/allManagedActivities")
    public Result getAllManagedActivities(Integer pageNum, Integer pageSize, Authentication authentication) {
        // 管理端查询只允许查看当前登录管理员负责的活动。
        String managerId = SecurityUtils.currentUserId(authentication);
        List<Activity> activityList = individualActivityService.getAllManagedActivitiesCached(managerId);
        return PageUtils.ok("items", activityList, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/accept")
    public Result acceptApplication(int activityId, String studentId, Authentication authentication) {
        // 使用认证管理员编号审批，防止通过请求参数越权审批其它活动。
        String managerId = SecurityUtils.currentUserId(authentication);
        individualActivityService.confirmApplicationAndNotify(activityId, studentId, managerId);
        return Result.ok().message("审批通过");
    }

    @RepeatLimit
    @PostMapping("/reject")
    public Result rejectApplication(int activityId, String studentId, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualActivityService.denyApplicationAndNotify(activityId, studentId, managerId);
        return Result.ok().message("已拒绝");
    }

    @RepeatLimit
    @RequestMapping("/getActivityMembers")
    public Result getActivityMembers() {
        List<ActivityNum> list = individualActivityService.getActivityMembersCached();
        return Result.ok().data("items", list);
    }
}
