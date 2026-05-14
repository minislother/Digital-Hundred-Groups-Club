package com.chinahitech.shop.mq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Kafka生产者单元测试")
@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducer kafkaProducer;

    @Test
    @DisplayName("发送消息 - Kafka关闭时跳过发送")
    void testSend_KafkaDisabled_SkipSend() {
        // Given - 准备测试数据和Mock行为
        ReflectionTestUtils.setField(kafkaProducer, "kafkaEnabled", false);

        // When - 执行被测试方法
        boolean result = kafkaProducer.send("activity_topic", "{\"id\":1}");

        // Then - 验证结果和交互
        assertFalse(result);
        verify(kafkaTemplate, never()).send("activity_topic", "{\"id\":1}");
    }

    @Test
    @DisplayName("发送消息 - Kafka开启时发送成功")
    void testSend_KafkaEnabled_SendSuccess() {
        // Given - 准备测试数据和Mock行为
        ReflectionTestUtils.setField(kafkaProducer, "kafkaEnabled", true);

        // When - 执行被测试方法
        boolean result = kafkaProducer.send("activity_topic", "{\"id\":1}");

        // Then - 验证结果和交互
        assertTrue(result);
        verify(kafkaTemplate).send("activity_topic", "{\"id\":1}");
    }

    @Test
    @DisplayName("发送消息 - Kafka发送异常返回失败")
    void testSend_KafkaException_ReturnFalse() {
        // Given - 准备测试数据和Mock行为
        ReflectionTestUtils.setField(kafkaProducer, "kafkaEnabled", true);
        when(kafkaTemplate.send("activity_topic", "{\"id\":1}"))
                .thenThrow(new RuntimeException("send failed"));

        // When - 执行被测试方法
        boolean result = kafkaProducer.send("activity_topic", "{\"id\":1}");

        // Then - 验证结果
        assertFalse(result);
    }
}
