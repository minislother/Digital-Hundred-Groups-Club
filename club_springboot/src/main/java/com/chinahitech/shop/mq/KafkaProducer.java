package com.chinahitech.shop.mq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

@Component
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public boolean send(String topic, String msg) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled. skip send. topic={}", topic);
            return false;
        }

        try {
            kafkaTemplate.send(topic, msg);
            return true;
        } catch (Exception e) {
            log.error("Kafka send failed. topic={}, msg={}", topic, msg, e);
            return false;
        }
    }
}
