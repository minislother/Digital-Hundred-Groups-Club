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

    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(String searchInfo, Integer pageNum, Integer pageSize) {
        List<Group> groups = groupService.queryCached(searchInfo);
        return PageUtils.ok("items", groups, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/studentDetail")
    public Result getStudentDetail(String groupName) {
        Group group = groupService.getCachedGroupDetail(groupName);
        return Result.ok().data("group", group);
    }

    @RepeatLimit
    @PostMapping("/addGroup")
    public Result addGroup(@RequestBody Group group) {
        groupService.insert(group);
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
        List<Group> groups = groupService.queryTopCached();
        return Result.ok().data("item", groups);
    }

    @RepeatLimit
    @PostMapping("/managerDetail")
    public Result getManagerDetail(String groupname) {
        Group group = groupService.getCachedGroupDetail(groupname);
        return Result.ok().data("group", group);
    }

    @RepeatLimit
    @PostMapping("/modifydescription")
    public Result modifyDescription(String groupname, String description, String attachment, String image) {
        groupService.updateDescription(groupname, description, attachment, image);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/uploadzip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    @RepeatLimit
    @PostMapping("/submitzip")
    public ResponseEntity<Map<String, String>> submitZip(@RequestParam("attachment") String attachment, @RequestParam("name") String name) {
        groupService.updateAttachment(name, attachment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated attachment.");
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @PostMapping("/uploadphoto")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileResponse(fileStorageService.store(file)));
    }

    @RepeatLimit
    @PostMapping("/submitphoto")
    public ResponseEntity<Map<String, String>> submitPhoto(@RequestParam("image") String image, @RequestParam("name") String name) {
        groupService.updateImage(name, image);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated image.");
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @PostMapping("/getattachment")
    public ResponseEntity<Map<String, Object>> getAttachment(@RequestParam("id") int id) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        response.put("success", true);
        response.put("attachment", groupService.getAttachment(id));
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @RequestMapping("/allApps")
    public Result getAllApps(String searchinfo, Integer pageNum, Integer pageSize) {
        List<Group> groups = groupService.getAllApp(searchinfo);
        return PageUtils.ok("items", groups, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/appDetail")
    public Result getAppDetail(String groupname) {
        Group group = groupService.getAppByName(groupname);
        return Result.ok().data("group", group);
    }

    @RepeatLimit
    @PostMapping("/accept")
    public Result accept(int groupId) {
        groupService.confirmApplication(groupId);
        return Result.ok();
    }

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
