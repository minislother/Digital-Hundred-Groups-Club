package com.chinahitech.shop.mq;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class KafkaConsumer {

    @Resource
    private ActivityService activityService;

    @KafkaListener(topics = MqTopics.ACTIVITY)
    public void consumeActivity(String message) {
        try {
            Map<String, Object> map = JSONUtil.toBean(message, Map.class);
            String type = String.valueOf(map.get("type"));
            Activity activity = JSONUtil.toBean(JSONUtil.toJsonStr(map.get("data")), Activity.class);

            if ("add".equals(type)) {
                activityService.insert(activity);
            } else {
                log.warn("Unknown activity message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Activity message consume failed. message={}", message, e);
        }
    }

    @KafkaListener(topics = MqTopics.ACTIVITY_CACHE)
    public void consumeActivityCacheEvict(String message) {
        try {
            Map<String, Object> map = JSONUtil.toBean(message, Map.class);
            String pattern = String.valueOf(map.getOrDefault("pattern", "activity:*"));
            RedisUtils.del(pattern);
        } catch (Exception e) {
            log.error("Activity cache evict retry failed. message={}", message, e);
        }
    }
}
