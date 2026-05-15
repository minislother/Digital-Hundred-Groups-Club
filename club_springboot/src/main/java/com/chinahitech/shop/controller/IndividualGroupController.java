package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;
import com.chinahitech.shop.service.IndividualGroupService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 个人社团关系接口。
 *
 * <p>学生端接口从认证信息中读取学生编号；管理端接口从认证信息中读取管理员编号，
 * 不信任请求参数中的 managerId，避免越权查询或操作其它管理员负责的社团。</p>
 */
@RestController
@RequestMapping("/individualGroup")
public class IndividualGroupController {

    @Autowired
    private IndividualGroupService individualGroupService;

    @RepeatLimit
    @RequestMapping("/allGroups")
    public Result getIndividualGroup(Authentication authentication) {
        String studentId = SecurityUtils.currentUserId(authentication);
        Map<String, Object> map = individualGroupService.getStudentGroupsCached(studentId);
        return Result.ok().data(map);
    }

    @RepeatLimit
    @RequestMapping("/applyJoinGroup")
    public Result applyJoinGroup(int groupId, Authentication authentication) {
        String studentId = SecurityUtils.currentUserId(authentication);
        individualGroupService.applyJoinGroupAndNotify(groupId, studentId);
        return Result.ok().message("申请提交成功，请等待管理员审核");
    }

    @RepeatLimit
    @RequestMapping("/getStudentsByGroup")
    public Result getStudentsByGroup(int groupId, String searchInfo, Integer pageNum, Integer pageSize) {
        List<IndividualGroup> studentList = individualGroupService.getStudentsByGroupCached(groupId, searchInfo);
        return PageUtils.ok("items", studentList, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/getGroupApplyList")
    public Result getGroupApplyList(int groupId, Integer pageNum, Integer pageSize) {
        List<IndividualGroup> applyList = individualGroupService.getGroupApplyListCached(groupId);
        return PageUtils.ok("items", applyList, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/acceptJoin")
    public Result acceptJoin(int groupId, String studentId, Authentication authentication) {
        // 使用当前登录管理员作为审批人，Service 会继续校验其是否管理该社团。
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.acceptJoinAndNotify(groupId, studentId, managerId);
        return Result.ok().message("已批准加入");
    }

    @RepeatLimit
    @RequestMapping("/rejectJoin")
    public Result rejectJoin(int groupId, String studentId, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.rejectJoinAndNotify(groupId, studentId, managerId);
        return Result.ok().message("已拒绝申请");
    }

    @RepeatLimit
    @RequestMapping("/addGroupStudent")
    public Result addGroupStudent(int groupId, String studentId, String position, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.addGroupStudentAndNotify(groupId, studentId, position, managerId);
        return Result.ok().message("添加成功");
    }

    @RepeatLimit
    @RequestMapping("/modifyGroupStudent")
    public Result modifyGroupStudent(int groupId, String studentId, String position, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.modifyGroupStudentAndNotify(groupId, studentId, position, managerId);
        return Result.ok().message("修改成功");
    }

    @RepeatLimit
    @RequestMapping("/deleteGroupStudent")
    public Result deleteGroupStudent(int groupId, String studentId, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.deleteGroupStudentAndNotify(groupId, studentId, managerId);
        return Result.ok().message("删除成功");
    }

    @RepeatLimit
    @RequestMapping("/allManagedGroups")
    public Result getAllManagedGroups(Integer pageNum, Integer pageSize, Authentication authentication) {
        // 管理端查询只允许查看当前登录管理员负责的社团。
        String managerId = SecurityUtils.currentUserId(authentication);
        List<Group> groupList = individualGroupService.getAllManagedGroupsCached(managerId);
        return PageUtils.ok("items", groupList, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/transferStatus")
    public Result transferStatus(int groupId, String userId, Authentication authentication) {
        // 原管理员必须是当前登录用户，防止通过请求参数冒充其它管理员转让权限。
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.transferStatusAndNotify(groupId, managerId, userId);
        return Result.ok().message("社团管理权限转让成功");
    }

    @RepeatLimit
    @RequestMapping("/updatePermission")
    public Result updatePermission(int groupId, String studentId, int status, Authentication authentication) {
        String managerId = SecurityUtils.currentUserId(authentication);
        individualGroupService.updatePermissionAndNotify(groupId, studentId, status, managerId);
        return Result.ok().message("权限修改成功");
    }

    @RepeatLimit
    @RequestMapping("/getAllStudents")
    public Result getAllStudents(String searchInfo, Integer pageNum, Integer pageSize) {
        List<IndividualGroup> studentList = individualGroupService.getAllStudents(searchInfo);
        return PageUtils.ok("items", studentList, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/getGroupMembers")
    public Result getGroupMembers() {
        List<GroupNum> groupList = individualGroupService.getGroupMembersCached();
        return Result.ok().data("items", groupList);
    }
}
