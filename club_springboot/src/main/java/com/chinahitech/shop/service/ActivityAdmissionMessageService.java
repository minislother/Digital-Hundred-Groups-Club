package com.chinahitech.shop.service;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 同一事务写入消费回执和报名记录，失败回滚后仍允许 Kafka 重试。 */
@Service
@RequiredArgsConstructor
public class ActivityAdmissionMessageService {
    private final ActivityAdmissionRecovery recovery;
    private final ActivityAdmissionService admissions;
    private final ActivityAdmissionMetrics metrics;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void consume(String payload) {
        Admission event = parse(payload);
        String token = event.versioned() ? event.token() : null;
        if (recovery.insertOnce(event.id(), "CONSUMED", null, event.activity(), event.student(), token)) {
            admissions.consumeAdmission(event.activity(), event.student(), event.position(), event.status());
            if (event.acceptedAt() > 0) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        metrics.recordCompletion(event.acceptedAt());
                    }
                });
            }
            if (token != null) recovery.releaseAfterCommit(event.activity(), event.student(), token);
        }
    }

    /** 恢复器只有在死信持久化成功后才返回，数据库异常时由 Kafka 再次尝试。 */
    @Transactional
    public void deadLetter(ConsumerRecord<?, ?> record) {
        String payload = String.valueOf(record.value());
        Admission event;
        try {
            event = parse(payload);
        } catch (RuntimeException malformed) {
            String source = record.topic() + ":" + record.partition() + ":" + record.offset();
            recovery.insertOnce("invalid-" + digest(source), "DEAD", payload, null, null, null);
            return;
        }
        // 旧消息仅带学号，无法证明 Redis 占位属于该次报名，留作人工核对。
        String token = event.versioned() ? event.token() : null;
        recovery.insertOnce(event.id(), "DEAD", payload, event.activity(), event.student(), token);
    }

    private static Admission parse(String payload) {
        var json = JSONUtil.parseObj(payload);
        Integer activity = json.getInt("activityId");
        String student = json.getStr("studentId");
        String token = json.getStr("reservationToken");
        if (activity == null || activity <= 0 || student == null || student.isBlank()
                || student.codePointCount(0, student.length()) > 100 || token == null || token.isBlank()) {
            throw new IllegalArgumentException("报名消息缺少有效的活动、学号或预占令牌");
        }
        Integer version = json.getInt("reservationVersion");
        boolean versioned = Integer.valueOf(2).equals(version);
        if (version != null && !versioned) {
            throw new IllegalArgumentException("不支持该报名预占协议版本");
        }
        if (versioned && !isReservationToken(token)) {
            throw new IllegalArgumentException("报名预占令牌格式无效");
        }
        String id = versioned ? token : "legacy-" + digest(activity + ":" + student + ":" + token);
        // Timing metadata is optional so older queued messages remain consumable.
        long acceptedAt = json.get("acceptedAtEpochMs") instanceof Number stamp ? stamp.longValue() : 0;
        return new Admission(id, activity, student, json.getStr("position", "成员"),
                json.getInt("status", 0), token, versioned, acceptedAt);
    }

    private static boolean isReservationToken(String token) {
        return token.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}");
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Admission(String id, int activity, String student, String position, int status, String token, boolean versioned, long acceptedAt) {}
}
