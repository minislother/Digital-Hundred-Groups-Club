package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.StuApp;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.StuAppService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/application")
public class StuAppController {
    @Autowired
    private StuAppService stuAppService;

    @Autowired
    private FileStorageService fileStorageService;

    @RepeatLimit
    @RequestMapping("/all")
    public Result getAll(Integer pageNum, Integer pageSize) {
        List<StuApp> stuApps = stuAppService.query();
        return PageUtils.ok("items", stuApps, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/myApps")
    public Result getMyApps(String stuNumber, Integer pageNum, Integer pageSize) {
        List<StuApp> stuApps = stuAppService.queryMyapp(stuNumber);
        return PageUtils.ok("items", stuApps, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/submit")
    public Result submit(@RequestBody StuApp stuApp) {
        stuAppService.submit(stuApp);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/recApps")
    public Result getRecApps(String groupName, Integer pageNum, Integer pageSize) {
        List<StuApp> stuApps = stuAppService.queryRecvapp(groupName);
        return PageUtils.ok("items", stuApps, pageNum, pageSize);
    }

    @RepeatLimit
    @PostMapping("/recApp")
    public Result getRecApp(Integer id) {
        StuApp stuApp = stuAppService.queryDetailapp(id);
        String isAcceptedStr = stuAppService.findIsAccepted(id);
        return Result.ok().data("items", stuApp).data("isAccepted", isAcceptedStr);
    }

    @RepeatLimit
    @PostMapping("/updateAttachment")
    public Result updateAttachment(@RequestParam("applicationId") int applicationId,
                                   @RequestParam("attachment") String attachmentUrl) {
        stuAppService.updateAttachment(applicationId, attachmentUrl);
        return Result.ok().data("applicationId", applicationId).data("attachment", attachmentUrl);
    }

    @RepeatLimit
    @PostMapping("/uploadZip")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (!("application/zip".equals(contentType) || "application/x-zip-compressed".equals(contentType))) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Only zip files are allowed.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        StoredFile storedFile = fileStorageService.store(file);
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", storedFile.getFileUrl());
        return ResponseEntity.ok(response);
    }

    @RepeatLimit
    @PostMapping("/accept")
    public Result acceptApplication(Integer applicationId) {
        stuAppService.confirmApplication(applicationId);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/reject")
    public Result rejectApplication(Integer applicationId) {
        stuAppService.denyApplication(applicationId);
        return Result.ok();
    }
}
