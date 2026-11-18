package com.chinahitech.shop.mapper;

import org.apache.ibatis.annotations.*;
import java.util.Map;

/** 搜索投影的持久化事件队列。触发器负责与活动写入原子提交，消费者只删除已成功事件。 */
public interface ActivitySearchOutboxMapper {
    @Select("SELECT id, activity_id AS activityId, attempts FROM activity_search_outbox " +
            "WHERE available_at <= NOW(3) ORDER BY available_at,id LIMIT 1 FOR UPDATE SKIP LOCKED")
    Map<String, Object> lockNext();

    @Delete("DELETE FROM activity_search_outbox WHERE id=#{id}")
    void complete(@Param("id") long id);

    @Update("UPDATE activity_search_outbox SET attempts=attempts+1, last_error=#{error}, " +
            "available_at=TIMESTAMPADD(SECOND,#{delay},NOW(3)) WHERE id=#{id}")
    void retry(@Param("id") long id, @Param("error") String error, @Param("delay") int delay);

    // 锁定读等待未提交的活动变更，防止回填的较大事件号先读取旧数据、反过来压住更新事件。
    @Insert("INSERT INTO activity_search_outbox(activity_id) SELECT id FROM activity FOR SHARE")
    int enqueueAll();

    @Select("SELECT COUNT(*) AS pending, COALESCE(SUM(attempts>0),0) AS retrying, " +
            "MIN(created_at) AS oldest FROM activity_search_outbox")
    Map<String, Object> status();

    @Select("SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE() " +
            "AND event_object_table='activity' AND trigger_name IN " +
            "('activity_search_insert','activity_search_update','activity_search_delete')")
    int triggerCount();
}
