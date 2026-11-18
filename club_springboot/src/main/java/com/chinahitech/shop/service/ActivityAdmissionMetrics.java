package com.chinahitech.shop.service;

import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper;
import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper.OutboxBacklog;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Samples shared database backlog away from both request handling and Prometheus scrapes. */
@Component
class ActivityAdmissionMetrics {
    private final ActivityAdmissionOutboxMapper mapper;
    private final Timer completion;
    private final boolean sampleBacklog;
    private volatile OutboxBacklog backlog;

    ActivityAdmissionMetrics(ActivityAdmissionOutboxMapper mapper, MeterRegistry registry,
            @Value("${app.activity.outbox.metrics-enabled:false}") boolean sampleBacklog) {
        this.mapper = mapper;
        this.sampleBacklog = sampleBacklog;
        completion = Timer.builder("baituan.admission.completion")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(10))
                .register(registry);
        if (sampleBacklog) {
            registry.gauge("baituan.admission.outbox.pending", this,
                    metrics -> {
                        var sample = metrics.backlog;
                        return sample == null ? Double.NaN : sample.pending();
                    });
            registry.gauge("baituan.admission.outbox.oldest.seconds", this,
                    metrics -> {
                        var sample = metrics.backlog;
                        return sample == null ? Double.NaN : sample.oldestSeconds();
                    });
        }
    }

    @Scheduled(initialDelayString = "${app.activity.outbox.metrics-interval-ms:30000}",
            fixedDelayString = "${app.activity.outbox.metrics-interval-ms:30000}")
    void sample() {
        if (sampleBacklog) {
            try {
                backlog = mapper.backlog();
            } catch (RuntimeException failure) {
                // Expose missing data rather than reporting the previous sample as current.
                backlog = null;
                throw failure;
            }
        }
    }

    void recordCompletion(long acceptedAt) {
        long elapsed = System.currentTimeMillis() - acceptedAt;
        if (elapsed >= 0) {
            completion.record(elapsed, TimeUnit.MILLISECONDS);
        }
    }
}
