package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.ActivityAdmissionOutbox;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("报名发件箱认领")
class ActivityAdmissionOutboxServiceTest {
    @Mock private ActivityAdmissionOutboxMapper mapper;
    @Mock private KafkaProducer producer;
    @InjectMocks private ActivityAdmissionOutboxService service;

    @Test
    @DisplayName("发送超时 - 按本次租约记录确认结果不明")
    void testDispatchPending_AckUncertain_DefersWithReason() {
        // Given
        var event = new ActivityAdmissionOutbox();
        event.setId("event-2");
        event.setPayload("{}");
        event.setNextAttemptAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        event.setClaimUntil(event.getNextAttemptAt().plusMinutes(2));
        when(mapper.findDue()).thenReturn(List.of(event));
        when(mapper.claim(event.getId(), event.getNextAttemptAt(), event.getClaimUntil())).thenReturn(1);
        when(producer.sendWithResult(MqTopics.ACTIVITY_ADMISSION, event.getPayload()))
                .thenReturn(KafkaProducer.SendResult.ACK_UNCERTAIN);

        // When
        service.dispatchPending();

        // Then
        verify(mapper).defer(event.getId(), event.getClaimUntil(), "ACK_UNCERTAIN");
        verify(mapper, never()).markSent(anyString(), any());
    }

    @Test
    @DisplayName("两个轮询者看到同一事件时只有认领成功者发送")
    void testDispatchPending_ClaimLost_SkipsSend() {
        // Given - 两个轮询者持有同一份到期记录，数据库只允许一次认领。
        ActivityAdmissionOutbox event = new ActivityAdmissionOutbox();
        event.setId("event-1");
        event.setPayload("{}");
        event.setNextAttemptAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        event.setClaimUntil(event.getNextAttemptAt().plusMinutes(2));
        when(mapper.findDue()).thenReturn(List.of(event));
        when(mapper.claim(event.getId(), event.getNextAttemptAt(), event.getClaimUntil())).thenReturn(1, 0);
        when(producer.sendWithResult(MqTopics.ACTIVITY_ADMISSION, event.getPayload())).thenReturn(KafkaProducer.SendResult.CONFIRMED);

        // When
        service.dispatchPending();
        service.dispatchPending();

        // Then
        verify(producer).sendWithResult(MqTopics.ACTIVITY_ADMISSION, event.getPayload());
        verify(mapper).markSent(event.getId(), event.getClaimUntil());
        verify(mapper, never()).defer(anyString(), any(), anyString());
    }
}
