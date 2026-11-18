package com.chinahitech.shop.service;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.ActivityAdmissionOutbox;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.mq.KafkaProducer.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/** 发件箱投递器：Kafka 成功后标记完成，失败保留记录等待下次重试。 */
@Service
@Slf4j
public class ActivityAdmissionOutboxService {
    @Resource private ActivityAdmissionOutboxMapper mapper;
    @Resource private KafkaProducer producer;
    @Value("${app.kafka.enabled:false}") private boolean kafkaEnabled;

    public void enqueue(int activityId, String studentId, String position, int status, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", activityId); payload.put("studentId", studentId);
        payload.put("position", position); payload.put("status", status);
        payload.put("reservationToken", token);
        payload.put("reservationVersion", 2);
        payload.put("acceptedAtEpochMs", System.currentTimeMillis());
        ActivityAdmissionOutbox event = new ActivityAdmissionOutbox();
        event.setId(UUID.randomUUID().toString());
        event.setPayload(JSONUtil.toJsonStr(payload));
        mapper.insert(event);
    }

    @Scheduled(initialDelayString = "${app.activity.outbox.interval-ms:3000}",
            fixedDelayString = "${app.activity.outbox.interval-ms:3000}")
    public void dispatchScheduled() {
        // Kafka 关闭期间保留历史待投递任务，不消耗重试额度。
        if (kafkaEnabled) dispatchPending();
    }

    void dispatchPending() {
        for (ActivityAdmissionOutbox event : mapper.findDue()) {
            // 数据库只允许一个实例把本次到期时间推进到租约截止时间。
            if (mapper.claim(event.getId(), event.getNextAttemptAt(), event.getClaimUntil()) != 1) {
                continue;
            }
            SendResult result = producer.sendWithResult(MqTopics.ACTIVITY_ADMISSION, event.getPayload());
            if (result == SendResult.CONFIRMED) {
                mapper.markSent(event.getId(), event.getClaimUntil());
            } else {
                log.warn("报名事件未确认投递，outboxId={}, result={}", event.getId(), result);
                mapper.defer(event.getId(), event.getClaimUntil(), result.name());
                if (result == SendResult.INTERRUPTED) return;
            }
        }
    }

    public List<ActivityAdmissionOutbox> deadLetters() { return mapper.findDead(); }
    public void retry(String id) { mapper.retryDead(id); }
    public void resolve(String id) { mapper.resolve(id); }
}
