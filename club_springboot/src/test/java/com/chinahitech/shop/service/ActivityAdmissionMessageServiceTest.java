package com.chinahitech.shop.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("报名消费回执与死信")
class ActivityAdmissionMessageServiceTest {
    private static final String TOKEN = "389891ab-b9c1-4c1f-9ea5-a3566ef0f63a";
    private static final String PAYLOAD = """
            {"activityId":1,"studentId":"s1","reservationToken":"%s","reservationVersion":2}
            """.formatted(TOKEN);

    @Mock private ActivityAdmissionRecovery recovery;
    @Mock private ActivityAdmissionService admissions;
    @InjectMocks private ActivityAdmissionMessageService service;

    @Test
    @DisplayName("同一事件重投时只落库一次")
    void testConsume_ReplayedReceipt_SkipsSecondInsert() {
        // Given - 首次持久化成功，第二次发现已存在的回执。
        when(recovery.insertOnce(TOKEN, "CONSUMED", null, 1, "s1", TOKEN)).thenReturn(true, false);

        // When
        service.consume(PAYLOAD);
        service.consume(PAYLOAD);

        // Then
        verify(admissions).consumeAdmission(1, "s1", "成员", 0);
        verify(recovery).releaseAfterCommit(1, "s1", TOKEN);
    }

    @Test
    @DisplayName("重试耗尽后死信与原始预占令牌一起持久化")
    void testDeadLetter_ValidPayload_PersistsCompensation() {
        // Given
        var record = new ConsumerRecord<>("activity_admission_topic", 0, 11, "key", PAYLOAD);

        // When
        service.deadLetter(record);

        // Then
        verify(recovery).insertOnce(TOKEN, "DEAD", PAYLOAD, 1, "s1", TOKEN);
        verifyNoInteractions(admissions);
    }

    @Test
    @DisplayName("死信写入失败时向Kafka抛出异常")
    void testDeadLetter_DatabaseUnavailable_Propagates() {
        // Given
        var record = new ConsumerRecord<>("activity_admission_topic", 0, 11, "key", PAYLOAD);
        when(recovery.insertOnce(TOKEN, "DEAD", PAYLOAD, 1, "s1", TOKEN))
                .thenThrow(new IllegalStateException("database unavailable"));

        // When / Then
        assertThrows(IllegalStateException.class, () -> service.deadLetter(record));
    }

    @Test
    @DisplayName("旧消息死信保留供人工核对")
    void testDeadLetter_LegacyPayload_DoesNotReleaseNewReservation() {
        // Given
        String legacy = PAYLOAD.replace(",\"reservationVersion\":2", "");
        var record = new ConsumerRecord<>("activity_admission_topic", 0, 12, "key", legacy);

        // When
        service.deadLetter(record);

        // Then
        verify(recovery).insertOnce(startsWith("legacy-"), eq("DEAD"), eq(legacy), eq(1), eq("s1"), isNull());
    }

    @Test
    @DisplayName("超长学号进入原始消息死信，避免死信列溢出阻塞分区")
    void testDeadLetter_OversizedStudent_PersistsRawPayload() {
        // Given
        String oversized = PAYLOAD.replace("s1", "s".repeat(300));
        var record = new ConsumerRecord<>("activity_admission_topic", 1, 13, "key", oversized);

        // When
        service.deadLetter(record);

        // Then
        verify(recovery).insertOnce(startsWith("invalid-"), eq("DEAD"), eq(oversized), isNull(), isNull(), isNull());
    }
}
