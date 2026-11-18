package com.chinahitech.shop.mq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    /** Stable categories suitable for logging and persisting without message content. */
    public enum SendResult {
        CONFIRMED, DISABLED, ACK_UNCERTAIN, INTERRUPTED, SEND_FAILED
    }

    public boolean send(String topic, String msg) {
        SendResult result = sendWithResult(topic, msg);
        if (result == SendResult.ACK_UNCERTAIN || result == SendResult.INTERRUPTED) {
            log.warn("Kafka send not confirmed, topic={}, result={}", topic, result);
        }
        return result == SendResult.CONFIRMED;
    }

    public SendResult sendWithResult(String topic, String msg) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled. skip send. topic={}", topic);
            return SendResult.DISABLED;
        }

        try {
            // A local wait timeout leaves delivery uncertain. The consumer deduplicates any replay.
            kafkaTemplate.send(topic, msg).get(3, TimeUnit.SECONDS);
            return SendResult.CONFIRMED;
        } catch (InterruptedException | org.apache.kafka.common.errors.InterruptException e) {
            Thread.currentThread().interrupt();
            return SendResult.INTERRUPTED;
        } catch (TimeoutException e) {
            return SendResult.ACK_UNCERTAIN;
        } catch (ExecutionException | RuntimeException e) {
            Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            log.warn("Kafka send failed, topic={}, causeType={}", topic, cause.getClass().getSimpleName());
            return cause instanceof org.apache.kafka.common.errors.TimeoutException
                    ? SendResult.ACK_UNCERTAIN : SendResult.SEND_FAILED;
        }
    }
}
