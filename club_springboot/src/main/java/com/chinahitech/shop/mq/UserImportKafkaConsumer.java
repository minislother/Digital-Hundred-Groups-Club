package com.chinahitech.shop.mq;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.service.TopManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class UserImportKafkaConsumer {

    @Resource
    private TopManagerService topManagerService;

    @KafkaListener(topics = MqTopics.USER_IMPORT)
    public void consume(String message) {
        try {
            Map<String, Object> map = JSONUtil.toBean(message, Map.class);
            String filePath = String.valueOf(map.get("filePath"));
            topManagerService.importExcelFile(filePath);
        } catch (Exception e) {
            log.error("User import message consume failed. message={}", message, e);
        }
    }
}
