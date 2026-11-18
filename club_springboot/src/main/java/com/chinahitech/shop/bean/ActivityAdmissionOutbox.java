package com.chinahitech.shop.bean;

import lombok.Data;
import java.time.LocalDateTime;

/** 报名事件发件箱：和业务事务同库提交，避免 Redis 预占后 Kafka 消息丢失。 */
@Data
public class ActivityAdmissionOutbox {
    private String id;
    private String payload;
    private int attempts;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime deadAt;
    private String lastError;
    private LocalDateTime claimUntil;
}
