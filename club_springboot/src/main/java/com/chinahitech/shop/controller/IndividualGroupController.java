package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;
import com.chinahitech.shop.service.IndividualGroupService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/individualGroup")
public class IndividualGroupController {

    @Autowired
    private IndividualGroupService individualGroupService;

    @RepeatLimit
    @RequestMapping("/allGroups")
    public Result getIndividualGroup(String studentId) {
        Map<String, Object> map = individualGroupService.getStudentGroupsCached(studentId);
        return Result.ok().data(map);
    }

    @RepeatLimit
    @RequestMapping("/applyJoinGroup")
    public Result applyJoinGroup(int groupId, String studentId) {
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
    public Result acceptJoin(int groupId, String studentId) {
        individualGroupService.acceptJoinAndNotify(groupId, studentId);
        return Result.ok().message("已批准加入");
    }

    @RepeatLimit
    @RequestMapping("/rejectJoin")
    public Result rejectJoin(int groupId, String studentId) {
        individualGroupService.rejectJoinAndNotify(groupId, studentId);
        return Result.ok().message("已拒绝申请");
    }

    @RepeatLimit
    @RequestMapping("/addGroupStudent")
    public Result addGroupStudent(int groupId, String studentId, String position) {
        individualGroupService.addGroupStudentAndNotify(groupId, studentId, position);
        return Result.ok().message("添加成功");
    }

    @RepeatLimit
    @RequestMapping("/modifyGroupStudent")
    public Result modifyGroupStudent(int groupId, String studentId, String position) {
        individualGroupService.modifyGroupStudentAndNotify(groupId, studentId, position);
        return Result.ok().message("修改成功");
    }

    @RepeatLimit
    @RequestMapping("/deleteGroupStudent")
    public Result deleteGroupStudent(int groupId, String studentId) {
        individualGroupService.deleteGroupStudentAndNotify(groupId, studentId);
        return Result.ok().message("删除成功");
    }

    @RepeatLimit
    @RequestMapping("/allManagedGroups")
    public Result getAllManagedGroups(String managerId, Integer pageNum, Integer pageSize) {
        List<Group> groupList = individualGroupService.getAllManagedGroupsCached(managerId);
        return PageUtils.ok("items", groupList, pageNum, pageSize);
    }

    @RepeatLimit
    @RequestMapping("/transferStatus")
    public Result transferStatus(int groupId, String managerId, String userId) {
        individualGroupService.transferStatusAndNotify(groupId, managerId, userId);
        return Result.ok().message("社团管理权限转让成功");
    }

    @RepeatLimit
    @RequestMapping("/updatePermission")
    public Result updatePermission(int groupId, String studentId, int status) {
        individualGroupService.updatePermissionAndNotify(groupId, studentId, status);
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
