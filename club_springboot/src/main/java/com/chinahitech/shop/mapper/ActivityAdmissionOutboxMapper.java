package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.ActivityAdmissionOutbox;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface ActivityAdmissionOutboxMapper {
    void insert(ActivityAdmissionOutbox event);
    List<ActivityAdmissionOutbox> findDue();
    OutboxBacklog backlog();

    record OutboxBacklog(long pending, long oldestSeconds) {}
    int claim(@Param("id") String id, @Param("dueAt") LocalDateTime dueAt,
              @Param("claimUntil") LocalDateTime claimUntil);
    void markSent(@Param("id") String id, @Param("claimUntil") LocalDateTime claimUntil);
    void defer(@Param("id") String id, @Param("claimUntil") LocalDateTime claimUntil,
               @Param("error") String error);
    List<ActivityAdmissionOutbox> findDead();
    void retryDead(@Param("id") String id);
    void resolve(@Param("id") String id);
}
