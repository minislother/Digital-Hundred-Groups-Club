package com.chinahitech.shop.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 持久化消费结论与待释放的预占，事件主键负责跨实例去重。 */
public interface ActivityAdmissionReceiptMapper {
    record PendingRelease(String eventId, int activityId, String studentId, String reservationToken) {}

    @Insert("""
            insert into activity_admission_receipt
                (event_id, kind, payload, activity_id, student_id, reservation_token, completed_at)
            values (#{id}, #{kind}, #{payload}, #{activity}, #{student}, #{token},
                    case when #{token} is null then now() else null end)
            """)
    void insert(@Param("id") String id, @Param("kind") String kind, @Param("payload") String payload,
                @Param("activity") Integer activity, @Param("student") String student, @Param("token") String token);

    @Select("""
            select event_id as eventId, activity_id as activityId, student_id as studentId,
                   reservation_token as reservationToken
            from activity_admission_receipt
            where completed_at is null and next_attempt_at <= now()
            order by next_attempt_at, event_id limit 100
            """)
    List<PendingRelease> findPending();

    @Update("update activity_admission_receipt set completed_at = now() where event_id = #{id} and completed_at is null")
    void complete(@Param("id") String id);

    @Update("""
            update activity_admission_receipt set next_attempt_at = date_add(now(), interval 30 second)
            where event_id = #{id} and completed_at is null
            """)
    void defer(@Param("id") String id);

    @Select("""
            select event_id as id, payload, activity_id as activityId, student_id as studentId,
                   completed_at as completedAt, created_at as deadAt,
                   case when reservation_token is null then 'MANUAL_REVIEW'
                        when completed_at is null then 'PENDING'
                        else 'COMPLETED' end as compensationStatus
            from activity_admission_receipt where kind = 'DEAD'
            order by created_at desc limit 200
            """)
    List<java.util.Map<String, Object>> findDead();
}
