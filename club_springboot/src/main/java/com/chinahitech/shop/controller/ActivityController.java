package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.chinahitech.shop.service.ActivitySearchService activitySearchService;

    /** Elasticsearch 多字段搜索；ES 未启用或不可用时自动回退 MySQL。 */
    @RepeatLimit
    @RequestMapping("/search")
    public Result search(String keyword, Integer pageNum, Integer pageSize) {
        return activitySearchService.searchPage(keyword, pageNum, pageSize);
    }

    /**
     * 查询活动列表，支持按关键字筛选，并对结果做分页返回。
     */
    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(String searchInfo, Integer pageNum, Integer pageSize) {
        PageResult<Activity> page = activityService.queryPageCached(searchInfo, pageNum, pageSize);
        return PageUtils.ok("items", page);
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
     * 管理端兼容旧前端路径：发布活动申请，等价于新增活动记录。
     */
    @RepeatLimit
    @PostMapping("/addActivity")
    public Result addManagerActivity(@RequestBody Activity activity) {
        activityService.insert(activity);
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
     * 管理端兼容旧前端路径：按社团查询活动列表，并支持前端传入关键字筛选。
     */
    @RepeatLimit
    @PostMapping("/myActivity")
    public Result getMyActivity(String groupName, String searchInfo, Integer pageNum, Integer pageSize) {
        return PageUtils.ok("items", activityService.getActivityByGroupNamePage(groupName, searchInfo, pageNum, pageSize));
    }

    /**
     * 管理端兼容旧前端路径：活动变更页读取本社活动列表。
     */
    @RepeatLimit
    @PostMapping("/myApps")
    public Result getMyApps(String groupName, String searchInfo, Integer pageNum, Integer pageSize) {
        return PageUtils.ok("items", activityService.getActivityByGroupNamePage(groupName, searchInfo, pageNum, pageSize));
    }

    /**
     * 管理端兼容旧前端路径：保存活动基础信息修改。
     */
    @RepeatLimit
    @PostMapping("/modifyInfo")
    public Result modifyInfo(@RequestBody Activity activity) {
        activityService.modifyInfo(activity);
        return Result.ok();
    }

    /**
     * 上传活动附件压缩包，返回文件可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadZip")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        return fileResponse(fileStorageService.store(file));
    }

    /**
     * 将已上传的附件地址绑定到指定活动。
     */
    @RepeatLimit
    @PostMapping("/submitZip")
    public Result submitZip(@RequestParam("attachment") String attachment, @RequestParam("name") String name) {
        activityService.updateAttachment(name, attachment);
        return Result.ok().message("Successfully updated attachment.");
    }

    /**
     * 上传活动封面图片，返回图片可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadPhoto")
    public Result uploadPhoto(@RequestParam("file") MultipartFile file) {
        return fileResponse(fileStorageService.store(file));
    }

    /**
     * 将已上传的图片地址设置为指定活动封面。
     */
    @RepeatLimit
    @PostMapping("/submitPhoto")
    public Result submitPhoto(@RequestParam("image") String image, @RequestParam("name") String name) {
        activityService.updateImage(name, image);
        return Result.ok().message("Successfully updated image.");
    }

    /**
     * 根据活动 ID 查询附件地址，供前端下载或展示附件入口。
     */
    @RepeatLimit
    @PostMapping("/getAttachment")
    public Result getAttachment(@RequestParam("id") int id) {
        return Result.ok().data("attachment", activityService.getAttachment(id));
    }

    /**
     * 超级管理员查询待审核活动列表，支持搜索和分页。
     */
    @RepeatLimit
    @RequestMapping("/allApps")
    public Result getAllApps(String searchinfo, Integer pageNum, Integer pageSize) {
        return PageUtils.ok("items", activityService.getAllAppPage(searchinfo, pageNum, pageSize));
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
    @RepeatLimit(failOpenOnRedisError = true)
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
        return PageUtils.ok("items", activityService.getMyJoinedActivitiesPage(studentId, pageNum, pageSize));
    }

    /**
     * 管理端查询指定活动的报名申请人列表，并分页返回。
     */
    @RepeatLimit
    @PostMapping("/getActivityApplicants")
    public Result getActivityApplicants(Integer activityId, Integer pageNum, Integer pageSize) {
        return PageUtils.ok("items", activityService.getActivityApplicantsPage(activityId, pageNum, pageSize));
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

    private Result fileResponse(StoredFile storedFile) {
        return Result.ok().data("fileUrl", storedFile.getFileUrl());
    }

    private Integer currentStudentId(Authentication authentication) {
        try {
            return Integer.valueOf(SecurityUtils.currentUserId(authentication));
        } catch (NumberFormatException e) {
            throw new BusinessException("PARAM_ERROR", "用户编号不合法");
        }
    }

    @RepeatLimit
    @RequestMapping("/searchSlice")
    public Result searchSlice(String keyword, Integer pageSize, String cursor) {
        return activitySearchService.searchSlice(keyword, pageSize, cursor);
    }
}
