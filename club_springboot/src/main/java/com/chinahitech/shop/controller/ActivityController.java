package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(String searchInfo, Integer pageNum, Integer pageSize) {
        List<Activity> activities = activityService.queryCached(searchInfo);
        return PageUtils.ok("items", activities, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/studentDetail")
    public Result getStudentDetail(String groupName, String activityName) {
        Activity activity = activityService.getCachedActivityDetail(activityName, groupName);
        return Result.ok().data("activity", activity);
    }

    @RepeatLimit
    @PostMapping("/addGroup")
    public Result addActivity(@RequestBody Activity group) {
        activityService.insert(group);
        return Result.ok();
    }

    @RepeatLimit
    @RequestMapping("/getVideo")
    public Result getVideo() {
        String videoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/")
                .path("videotest.mp4")
                .toUriString();
        return Result.ok().data("url", videoUrl);
    }

    @RepeatLimit
    @RequestMapping("/top")
    public Result getTop() {
        List<Activity> activities = activityService.queryTopCached();
        return Result.ok().data("item", activities);
    }

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

    @RepeatLimit
    @PostMapping("/uploadZip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    @RepeatLimit
    @PostMapping("/submitZip")
    public ResponseEntity<Map<String, String>> submitZip(@RequestParam("attachment") String attachment, @RequestParam("name") String name) {
        activityService.updateAttachment(name, attachment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated attachment.");
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @PostMapping("/uploadPhoto")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    @RepeatLimit
    @PostMapping("/submitPhoto")
    public ResponseEntity<Map<String, String>> submitPhoto(@RequestParam("image") String image, @RequestParam("name") String name) {
        activityService.updateImage(name, image);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated image.");
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @PostMapping("/getAttachment")
    public ResponseEntity<Map<String, Object>> getAttachment(@RequestParam("id") int id) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        response.put("success", true);
        response.put("attachment", activityService.getAttachment(id));
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @RequestMapping("/allApps")
    public Result getAllApps(String searchinfo, Integer pageNum, Integer pageSize) {
        List<Activity> activities = activityService.getAllApp(searchinfo);
        return PageUtils.ok("items", activities, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/appDetail")
    public Result getAppDetail(String groupname) {
        Activity activity = activityService.getAppByName(groupname);
        return Result.ok().data("activity", activity);
    }

    @RepeatLimit
    @PostMapping("/accept")
    public Result accept(int activityId) {
        activityService.confirmApplication(activityId);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/reject")
    public Result reject(int activityId) {
        activityService.denyApplication(activityId);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/applyJoin")
    public Result applyJoin(Integer activityId, Integer studentId) {
        if (activityId == null || studentId == null) {
            throw new BusinessException("PARAM_ERROR", "参数不能为空");
        }
        activityService.applyJoin(activityId, studentId);
        return Result.ok().message("报名申请已提交");
    }

    @RepeatLimit
    @PostMapping("/myJoinedActivities")
    public Result myJoinedActivities(Integer studentId, Integer pageNum, Integer pageSize) {
        List<Activity> list = activityService.getMyJoinedActivities(studentId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/getActivityApplicants")
    public Result getActivityApplicants(Integer activityId, Integer pageNum, Integer pageSize) {
        List<Map<String, Object>> list = activityService.getActivityApplicants(activityId);
        return PageUtils.ok("items", list, pageNum, pageSize);
    }

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
}
