package com.chinahitech.shop.mq;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GroupKafkaConsumer {

    /**
     * 消费社团相关消息（新增社团）
     * 监听主题：group_topic
     */
    @KafkaListener(topics = MqTopics.GROUP, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeGroupMessage(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            log.info("接收到社团Kafka消息：{}", message);

            // 解析消息
            Map<String, Object> msgMap = JSONUtil.toBean(message, Map.class);
            String type = (String) msgMap.get("type");
            Group group = JSONUtil.toBean(JSONUtil.toJsonStr(msgMap.get("data")), Group.class);

            // 根据消息类型处理业务
            switch (type) {
                case "addGroup":
                    handleAddGroup(group);
                    break;
                default:
                    log.warn("未知的社团消息类型：{}", type);
            }
        } catch (Exception e) {
            log.error("消费社团消息失败", e);
        }
    }

    @KafkaListener(topics = MqTopics.GROUP_CACHE, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeGroupCacheEvict(String message) {
        try {
            Map<String, Object> map = JSONUtil.toBean(message, Map.class);
            String pattern = String.valueOf(map.getOrDefault("pattern", "group:*"));
            RedisUtils.del(pattern);
        } catch (Exception e) {
            log.error("Group cache evict retry failed. message={}", message, e);
        }
    }

    /**
     * 处理新增社团的业务逻辑（示例：日志记录、数据同步、通知等）
     */
    private void handleAddGroup(Group group) {
        log.info("处理新增社团业务：社团名称={}, ID={}", group.getName(), group.getId());
        // 1. 记录操作日志
        // 2. 同步到其他系统（如ES、数据仓库）
        // 3. 发送通知（邮件/短信）
        // 4. 其他自定义业务...
    }
}
