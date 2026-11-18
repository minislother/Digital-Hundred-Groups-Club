package com.chinahitech.shop.service;

import com.chinahitech.shop.mapper.ActivityAdmissionReceiptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/** 独立提交回滚补偿，后台持续重试直到 Redis 释放成功或确认令牌已失效。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAdmissionRecovery {
    private final ActivityAdmissionReceiptMapper receipts;
    private final StringRedisTemplate redis;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRollback(int activityId, String studentId, String token) {
        insertOnce(token, "ROLLBACK", null, activityId, studentId, token);
    }

    void recordSynchronousSuccess(int activityId, String studentId, String token) {
        if (insertOnce(token, "SYNC", null, activityId, studentId, token)) {
            releaseAfterCommit(activityId, studentId, token);
        }
    }

    boolean insertOnce(String id, String kind, String payload, Integer activity, String student, String token) {
        try {
            receipts.insert(id, kind, payload, activity, student, token);
            return true;
        } catch (DuplicateKeyException alreadyRecorded) {
            return false;
        }
    }

    void releaseAfterCommit(int activityId, String studentId, String token) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    release(activityId, studentId, token);
                } catch (RuntimeException failure) {
                    log.warn("报名已提交，后台将重试预占释放，事件={}", token, failure);
                }
            }
        });
    }

    private void release(int activityId, String studentId, String token) {
        Long result = redis.execute(ActivityAdmissionService.RELEASE,
                ActivityAdmissionService.reservationKeys(activityId), studentId, token);
        if (result == null) throw new IllegalStateException("Redis 未确认预占释放");
    }

    public List<Map<String, Object>> deadLetters() {
        return receipts.findDead();
    }

    @Scheduled(initialDelayString = "${app.activity.compensation.interval-ms:30000}",
               fixedDelayString = "${app.activity.compensation.interval-ms:30000}")
    public void releasePending() {
        for (var task : receipts.findPending()) {
            try {
                release(task.activityId(), task.studentId(), task.reservationToken());
                receipts.complete(task.eventId());
            } catch (RuntimeException failure) {
                receipts.defer(task.eventId());
                log.warn("报名预占补偿待重试，事件={}", task.eventId(), failure);
                // Redis 故障时每轮只探测一次，避免阻塞同一个调度线程上的其他任务。
                return;
            }
        }
    }
}
