package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.Inspection;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.InspectionService;
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

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspection")
public class InspectionController {
    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 社团管理员查询本社团的年审记录，并分页返回。
     */
    @RepeatLimit
    @RequestMapping("/findAllByGroup")
    public Result findAllByGroup(String groupName, Integer pageNum, Integer pageSize) {
        List<Inspection> inspections = inspectionService.findAllByGroup(groupName);
        return PageUtils.ok("items", inspections, pageNum, pageSize);
    }

    /**
     * 社团管理员按年份查询本社团的年审记录，并分页返回。
     */
    @RepeatLimit
    @RequestMapping("/findAllByGroupAndYear")
    public Result findAllByGroupAndYear(String groupName, Year year, Integer pageNum, Integer pageSize) {
        List<Inspection> inspections = inspectionService.findAllByGroupAndYear(groupName, year);
        return PageUtils.ok("items", inspections, pageNum, pageSize);
    }

    /**
     * 社团管理员提交新的年审材料记录。
     */
    @RepeatLimit
    @PostMapping("/addInspection")
    public Result addInspection(Inspection inspection) {
        inspectionService.addInspection(inspection);
        return Result.ok();
    }

    /**
     * 社团管理员修改年审材料中的附件信息。
     */
    @RepeatLimit
    @PostMapping("/modifyInfo")
    public Result modifyInfo(Inspection inspection) {
        inspectionService.modifyAttachment(inspection.getId(), inspection.getGroupName(), inspection.getAttachment());
        return Result.ok();
    }

    /**
     * 上传年审附件压缩包，返回文件可访问地址。
     */
    @RepeatLimit
    @PostMapping("/uploadZip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        StoredFile storedFile = fileStorageService.store(file);
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", storedFile.getFileUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * 将已上传的附件地址绑定到指定年审记录。
     */
    @RepeatLimit
    @PostMapping("/submitZip")
    public ResponseEntity<Map<String, String>> submitZip(@RequestParam("inspectionId") int inspectionId,
                                                         @RequestParam("attachment") String attachment,
                                                         @RequestParam("groupName") String groupName) {
        inspectionService.modifyAttachment(inspectionId, groupName, attachment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully updated attachment.");
        return ResponseEntity.ok(response);
    }

    /**
     * 根据年审记录 ID 查询附件地址，供前端下载或展示附件入口。
     */
    @RepeatLimit
    @PostMapping("/getAttachment")
    public ResponseEntity<Map<String, Object>> getAttachment(@RequestParam("id") int id) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        response.put("success", true);
        response.put("attachment", inspectionService.getAttachment(id));
        return ResponseEntity.ok(response);
    }

    /**
     * 超级管理员查询全部年审记录，支持搜索和分页。
     */
    @RepeatLimit
    @RequestMapping("/all")
    public Result findAll(String searchInfo, Integer pageNum, Integer pageSize) {
        List<Inspection> inspections = inspectionService.findAll(searchInfo);
        return PageUtils.ok("items", inspections, pageNum, pageSize);
    }

    /**
     * 超级管理员按年份查询全部年审记录，并分页返回。
     */
    @RepeatLimit
    @RequestMapping("/findAllByYear")
    public Result findAllByYear(Year year, Integer pageNum, Integer pageSize) {
        List<Inspection> inspections = inspectionService.findAllByYear(year);
        return PageUtils.ok("items", inspections, pageNum, pageSize);
    }

    /**
     * 超级管理员为年审记录填写反馈意见。
     */
    @RepeatLimit
    @PostMapping("/addFeedback")
    public Result addFeedback(@RequestBody Inspection inspection) {
        inspectionService.addFeedback(inspection.getId(), inspection.getGroupName(), inspection.getFeedback());
        return Result.ok();
    }

    /**
     * 超级管理员通过年审申请。
     */
    @RepeatLimit
    @PostMapping("/accept")
    public Result accept(int inspectionId) {
        inspectionService.confirmApplication(inspectionId);
        return Result.ok();
    }

    /**
     * 超级管理员拒绝年审申请。
     */
    @RepeatLimit
    @PostMapping("/reject")
    public Result reject(int inspectionId) {
        inspectionService.denyApplication(inspectionId);
        return Result.ok();
    }
}
