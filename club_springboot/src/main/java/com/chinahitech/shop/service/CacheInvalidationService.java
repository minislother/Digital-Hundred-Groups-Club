package com.chinahitech.shop.service;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.CacheInvalidationTask;
import com.chinahitech.shop.mapper.CacheInvalidationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Arrays;
import java.util.UUID;

/** 在业务事务内记录删除意图，提交后才访问 Redis；回滚时任务也随之回滚。 */
@Service
public class CacheInvalidationService {
    @Resource private CacheInvalidationMapper mapper;
    @Resource private CacheInvalidationWorker worker;

    @Transactional(rollbackFor = Exception.class)
    public void evict(String... patterns) {
        if (patterns == null || patterns.length == 0
                || Arrays.stream(patterns).anyMatch(p -> p == null || p.trim().isEmpty())) {
            throw new IllegalArgumentException("缓存删除范围不能为空");
        }
        CacheInvalidationTask task = new CacheInvalidationTask();
        task.setId(UUID.randomUUID().toString());
        task.setPatterns(JSONUtil.toJsonStr(patterns));
        mapper.insert(task);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { worker.tryProcess(task); }
            });
        } else {
            worker.tryProcess(task);
        }
    }
}
