package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.ActivitySearchService;
import com.chinahitech.shop.utils.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;

/** 搜索索引维护接口；重建操作应只授予超级管理员并在低峰期执行。 */
@RestController
@RequestMapping("/search/index")
public class ActivitySearchController {
    @Resource private ActivitySearchService searchService;

    @PostMapping("/rebuildActivities")
    public Result rebuildActivities() {
        return Result.ok().data("queued", searchService.rebuild()).data("state", "QUEUED");
    }

    @org.springframework.web.bind.annotation.GetMapping("/status")
    public Result status() { return Result.ok().data("queue", searchService.status()); }
}
