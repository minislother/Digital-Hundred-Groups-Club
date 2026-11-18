package com.chinahitech.shop.service;

import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper;
import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper.OutboxBacklog;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("活动报名指标")
class ActivityAdmissionMetricsTest {
    @Test
    @DisplayName("积压采样 - 数据库故障后不继续展示旧值")
    void testSample_DatabaseFailure_InvalidatesSnapshot() {
        // Given
        var mapper = mock(ActivityAdmissionOutboxMapper.class);
        when(mapper.backlog()).thenReturn(new OutboxBacklog(3, 42)).thenThrow(new IllegalStateException("offline"));
        var registry = new SimpleMeterRegistry();
        try {
            var metrics = new ActivityAdmissionMetrics(mapper, registry, true);

            // When
            metrics.sample();
            var pending = registry.get("baituan.admission.outbox.pending").gauge();
            var oldest = registry.get("baituan.admission.outbox.oldest.seconds").gauge();

            // Then
            assertEquals(3d, pending.value());
            assertEquals(42d, oldest.value());
            assertThrows(IllegalStateException.class, metrics::sample);
            assertTrue(Double.isNaN(pending.value()));
        } finally {
            registry.close();
        }
    }
}
