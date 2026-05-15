package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.GroupService;
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
@RequestMapping("/group")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 查询社团列表，支持按关键字筛选，并对结果做分页返回。
     */
    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(String searchInfo, Integer pageNum, Integer pageSize) {
        List<Group> groups = groupService.queryCached(searchInfo);
        return PageUtils.ok("items", groups, pageNum, pageSize);
    }

    /**
     * 学生端查看指定社团的详情信息。
     */
    @RepeatLimit
    @PostMapping("/studentDetail")
    public Result getStudentDetail(String groupName) {
        Group group = groupService.getCachedGroupDetail(groupName);
        return Result.ok().data("group", group);
    }

    /**
     * 新增社团申请或社团记录，接收前端提交的社团对象并写入数据库。
     */
    @RepeatLimit
    @PostMapping("/addGroup")
    public Result addGroup(@RequestBody Group group) {
        groupService.insert(group);
        return Result.ok();
    }

    /**
     * 返回社团展示视频的访问地址，供前端播放固定宣传视频。
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
     * 查询成员数排名靠前的社团，用于首页或数据展示区域。
     */
    @RepeatLimit
    @RequestMapping("/top")
    public Result getTop() {
        List<Group> groups = groupService.queryTopCached();
        return Result.ok().data("item", groups);
    }

    /**
     * 管理端查看指定社团详情。
     */
    @RepeatLimit
    @PostMapping("/managerDetail")
    public Result getManagerDetail(String groupname) {
        Group group = groupService.getCachedGroupDetail(groupname);
        return Result.ok().data("group", group);
    }

    /**
     * 管理端修改社团简介、附件地址和封面图片地址。
     */
    @RepeatLimit
    @PostMapping("/modifydescription")
    public Result modifyDescription(String groupname, String description, String attachment, String image) {
        groupService.updateDescription(groupname, description, attachment, image);
        return Result.ok();
    }

    /**
     * 上传社团附件压缩包，返回文件可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadzip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    /**
     * 将已上传的附件地址绑定到指定社团。
     */
    @RepeatLimit
    @PostMapping("/submitzip")
    public ResponseEntity<Map<String, String>> submitZip(@RequestParam("attachment") String attachment, @RequestParam("name") String name) {
        groupService.updateAttachment(name, attachment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated attachment.");
        return ResponseEntity.ok(response);
    }

    /**
     * 上传社团封面图片，返回图片可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadphoto")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    /**
     * 将已上传的图片地址设置为指定社团封面。
     */
    @RepeatLimit
    @PostMapping("/submitphoto")
    public ResponseEntity<Map<String, String>> submitPhoto(@RequestParam("image") String image, @RequestParam("name") String name) {
        groupService.updateImage(name, image);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated image.");
        return ResponseEntity.ok(response);
    }

    /**
     * 根据社团 ID 查询附件地址，供前端下载或展示附件入口。
     */
    @RepeatLimit
    @PostMapping("/getattachment")
    public ResponseEntity<Map<String, Object>> getAttachment(@RequestParam("id") int id) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        response.put("success", true);
        response.put("attachment", groupService.getAttachment(id));
        return ResponseEntity.ok(response);
    }

    /**
     * 超级管理员查询待审核社团申请列表，支持搜索和分页。
     */
    @RepeatLimit
    @RequestMapping("/allApps")
    public Result getAllApps(String searchinfo, Integer pageNum, Integer pageSize) {
        List<Group> groups = groupService.getAllApp(searchinfo);
        return PageUtils.ok("items", groups, pageNum, pageSize);
    }

    /**
     * 超级管理员查看某个待审核社团的申请详情。
     */
    @RepeatLimit
    @PostMapping("/appDetail")
    public Result getAppDetail(String groupname) {
        Group group = groupService.getAppByName(groupname);
        return Result.ok().data("group", group);
    }

    /**
     * 超级管理员通过社团创建申请。
     */
    @RepeatLimit
    @PostMapping("/accept")
    public Result accept(int groupId) {
        groupService.confirmApplication(groupId);
        return Result.ok();
    }

    /**
     * 超级管理员拒绝社团创建申请。
     */
    @RepeatLimit
    @PostMapping("/reject")
    public Result reject(int groupId) {
        groupService.denyApplication(groupId);
        return Result.ok();
    }

    private Map<String, String> fileResponse(StoredFile storedFile) {
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", storedFile.getFileUrl());
        return response;
    }
}
