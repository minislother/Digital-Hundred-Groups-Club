package com.chinahitech.shop.service;

import com.chinahitech.shop.mapper.ActivityAdmissionReceiptMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("报名补偿轮询")
class ActivityAdmissionRecoveryTest {
    @Mock private ActivityAdmissionReceiptMapper receipts;
    @Mock private StringRedisTemplate redis;
    @InjectMocks private ActivityAdmissionRecovery recovery;

    @Test
    @DisplayName("Redis故障后保留任务，恢复时旧令牌已释放也可完成")
    void testReleasePending_RedisFailureThenAlreadyReleased_Completes() {
        // Given
        var task = new ActivityAdmissionReceiptMapper.PendingRelease("event-1", 1, "s1", "token-1");
        when(receipts.findPending()).thenReturn(List.of(task));
        when(redis.execute(ActivityAdmissionService.RELEASE, ActivityAdmissionService.reservationKeys(1),
                "s1", "token-1")).thenThrow(new IllegalStateException("offline")).thenReturn(0L);

        // When
        recovery.releasePending();
        recovery.releasePending();

        // Then
        var order = inOrder(receipts);
        order.verify(receipts).defer("event-1");
        order.verify(receipts).complete("event-1");
    }
}
