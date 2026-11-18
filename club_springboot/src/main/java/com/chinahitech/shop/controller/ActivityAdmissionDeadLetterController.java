package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.ActivityAdmissionOutboxService;
import com.chinahitech.shop.utils.Result;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

/** 超级管理员人工处理报名死信：查看、重试或标记已补偿。 */
@RestController
@RequestMapping("/admin/activity-admission/dead-letters")
public class ActivityAdmissionDeadLetterController {
    @Resource private ActivityAdmissionOutboxService service;

    @Resource private com.chinahitech.shop.service.ActivityAdmissionRecovery recovery;

    @GetMapping
    public Result list() { return Result.ok().data("items", service.deadLetters()).data("consumerItems", recovery.deadLetters()); }

    @PostMapping("/{id}/retry")
    public Result retry(@PathVariable String id) { service.retry(id); return Result.ok(); }

    @PostMapping("/{id}/resolve")
    public Result resolve(@PathVariable String id) { service.resolve(id); return Result.ok(); }
}
