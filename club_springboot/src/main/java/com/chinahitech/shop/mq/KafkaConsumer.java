package com.chinahitech.shop.mq;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class KafkaConsumer {

    @Resource
    private ActivityService activityService;

    @Resource
    private com.chinahitech.shop.service.CacheInvalidationWorker cacheWorker;

    @Resource
    private com.chinahitech.shop.service.ActivityAdmissionMessageService admissions;

    @KafkaListener(topics = MqTopics.ACTIVITY_ADMISSION, containerFactory = "admissionKafkaListenerContainerFactory")
    public void consumeActivityAdmission(String message) {
        admissions.consume(message);
    }

    @KafkaListener(topics = MqTopics.CACHE_RETRY, containerFactory = "cacheRetryKafkaListenerContainerFactory")
    public void consumeCacheRetry(String message) {
        String taskId = JSONUtil.parseObj(message).getStr("taskId");
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("缓存补偿消息缺少任务编号");
        }
        cacheWorker.retry(taskId);
    }

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
            // 兼容升级前积压的消息，但旧 activity:* 不能再删除报名控制数据。
            for (String pattern : new String[]{"activity:all:*", "activity:page:*", "activity:detail:*",
                    "activity:top", "activity:managed:*", "individualActivity:*"}) {
                if (!RedisUtils.del(pattern)) { throw new IllegalStateException("缓存删除失败: " + pattern); }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Activity cache evict retry failed", e);
        }
    }
}
