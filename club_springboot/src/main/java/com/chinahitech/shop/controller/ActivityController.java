package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 查询活动列表，支持按关键字筛选，并对结果做分页返回。
     */
    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(String searchInfo, Integer pageNum, Integer pageSize) {
        List<Activity> activities = activityService.queryCached(searchInfo);
        return PageUtils.ok("items", activities, pageNum, pageSize);
    }

    /**
     * 学生端查看指定社团下某个活动的详情。
     */
    @RepeatLimit
    @PostMapping("/studentDetail")
    public Result getStudentDetail(String groupName, String activityName) {
        Activity activity = activityService.getCachedActivityDetail(activityName, groupName);
        return Result.ok().data("activity", activity);
    }

    /**
     * 新增活动申请或活动记录，接收前端提交的活动对象并写入数据库。
     */
    @RepeatLimit
    @PostMapping("/addGroup")
    public Result addActivity(@RequestBody Activity group) {
        activityService.insert(group);
        return Result.ok();
    }

    /**
     * 返回活动展示视频的访问地址，供前端播放固定宣传视频。
     */
    @RepeatLimit
    @RequestMapping("/getVideo")
    public Result getVideo() {
        String videoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/")
                .path("videotest.mp4")
                .toUriString();
        return Result.ok().data("url", videoUrl);
    }

    /**
     * 查询报名人数排名靠前的活动，用于首页或数据展示区域。
     */
    @RepeatLimit
    @RequestMapping("/top")
    public Result getTop() {
        List<Activity> activities = activityService.queryTopCached();
        return Result.ok().data("item", activities);
    }

    /**
     * 管理端查看指定社团下某个活动的详情，参数为空时直接拒绝请求。
     */
    @RepeatLimit
    @PostMapping("/managerDetail")
    public Result getManagerDetail(String activityName, String groupName) {
        if (activityName == null || activityName.trim().isEmpty()
                || groupName == null || groupName.trim().isEmpty()) {
            throw new BusinessException("PARAM_ERROR", "参数不能为空");
        }
        Activity activity = activityService.getActivityByNameAndGroupName(activityName, groupName);
        return Result.ok().data("activity", activity);
    }

    /**
     * 管理端修改活动简介、附件地址和封面图片地址。
     */
    @RepeatLimit
    @PostMapping("/modifyDescription")
    public Result modifyDescription(String groupName, String activityName, String description, String attachment, String image) {
        if (groupName == null || groupName.trim().isEmpty()
                || activityName == null || activityName.trim().isEmpty()) {
            throw new BusinessException("PARAM_ERROR", "参数不能为空");
        }
        activityService.updateDescription(groupName, activityName, description, attachment, image);
        return Result.ok();
    }

    /**
     * 上传活动附件压缩包，返回文件可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadZip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    /**
     * 将已上传的附件地址绑定到指定活动。
     */
    @RepeatLimit
    @PostMapping("/submitZip")
    public ResponseEntity<Map<String, String>> submitZip(@RequestParam("attachment") String attachment, @RequestParam("name") String name) {
        activityService.updateAttachment(name, attachment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated attachment.");
        return ResponseEntity.ok(response);
    }

    /**
     * 上传活动封面图片，返回图片可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadPhoto")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    /**
     * 将已上传的图片地址设置为指定活动封面。
     */
    @RepeatLimit
    @PostMapping("/submitPhoto")
    public ResponseEntity<Map<String, String>> submitPhoto(@RequestParam("image") String image, @RequestParam("name") String name) {
        activityService.updateImage(name, image);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated image.");
        return ResponseEntity.ok(response);
    }

    /**
     * 根据活动 ID 查询附件地址，供前端下载或展示附件入口。
     */
    @RepeatLimit
    @PostMapping("/getAttachment")
    public ResponseEntity<Map<String, Object>> getAttachment(@RequestParam("id") int id) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        response.put("success", true);
        response.put("attachment", activityService.getAttachment(id));
        return ResponseEntity.ok(response);
    }

    /**
     * 超级管理员查询待审核活动列表，支持搜索和分页。
     */
    @RepeatLimit
    @RequestMapping("/allApps")
    public Result getAllApps(String searchinfo, Integer pageNum, Integer pageSize) {
        List<Activity> activities = activityService.getAllApp(searchinfo);
        return PageUtils.ok("items", activities, pageNum, pageSize);
    }

    /**
     * 超级管理员查看某个待审核活动的申请详情。
     */
    @RepeatLimit
    @PostMapping("/appDetail")
    public Result getAppDetail(String groupname) {
        Activity activity = activityService.getAppByName(groupname);
        return Result.ok().data("activity", activity);
    }

    /**
     * 超级管理员通过活动创建申请。
     */
    @RepeatLimit
    @PostMapping("/accept")
    public Result accept(int activityId) {
        activityService.confirmApplication(activityId);
        return Result.ok();
    }

    /**
     * 超级管理员拒绝活动创建申请。
     */
    @RepeatLimit
    @PostMapping("/reject")
    public Result reject(int activityId) {
        activityService.denyApplication(activityId);
        return Result.ok();
    }

    /**
     * 学生报名活动，使用当前登录学生编号提交报名申请。
     */
    @RepeatLimit
    @PostMapping("/applyJoin")
    public Result applyJoin(Integer activityId, Authentication authentication) {
        if (activityId == null) {
            throw new BusinessException("PARAM_ERROR", "参数不能为空");
        }
        Integer studentId = currentStudentId(authentication);
        activityService.applyJoin(activityId, studentId);
        return Result.ok().message("报名申请已提交");
    }

    /**
     * 查询当前登录学生已报名或已加入的活动列表，并分页返回。
     */
    @RepeatLimit
    @PostMapping("/myJoinedActivities")
    public Result myJoinedActivities(Integer pageNum, Integer pageSize, Authentication authentication) {
        Integer studentId = currentStudentId(authentication);
        List<Activity> list = activityService.getMyJoinedActivities(studentId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    /**
     * 管理端查询指定活动的报名申请人列表，并分页返回。
     */
    @RepeatLimit
    @PostMapping("/getActivityApplicants")
    public Result getActivityApplicants(Integer activityId, Integer pageNum, Integer pageSize) {
        List<Map<String, Object>> list = activityService.getActivityApplicants(activityId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    /**
     * 管理端审核活动报名申请，将申请记录更新为指定状态。
     */
    @RepeatLimit
    @PostMapping("/auditApply")
    public Result auditApply(Integer id, Integer status) {
        activityService.auditApply(id, status);
        return Result.ok();
    }

    private Map<String, String> fileResponse(StoredFile storedFile) {
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", storedFile.getFileUrl());
        return response;
    }

    private Integer currentStudentId(Authentication authentication) {
        try {
            return Integer.valueOf(SecurityUtils.currentUserId(authentication));
        } catch (NumberFormatException e) {
            throw new BusinessException("PARAM_ERROR", "用户编号不合法");
        }
    }
}
