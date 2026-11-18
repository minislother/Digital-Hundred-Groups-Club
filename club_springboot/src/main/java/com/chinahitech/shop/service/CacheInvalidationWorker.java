package com.chinahitech.shop.service;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.CacheInvalidationTask;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.mapper.CacheInvalidationMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Collections;

/** Redis 删除可重复执行。Kafka 只负责加速重试，数据库任务才是可靠的补偿凭据。 */
@Slf4j
@Service
public class CacheInvalidationWorker {
    @Resource private CacheInvalidationMapper mapper;
    @Resource private KafkaProducer producer;
    @Resource private PlatformTransactionManager transactionManager;

    public void tryProcess(CacheInvalidationTask task) {
        process(task, true);
    }

    private void process(CacheInvalidationTask task, boolean publishRetry) {
        try {
            boolean deleted = true;
            for (String pattern : JSONUtil.toList(task.getPatterns(), String.class)) {
                deleted &= RedisUtils.del(pattern);
            }
            final boolean success = deleted;
            // afterCommit 仍可能绑定已提交的连接，补偿任务更新必须使用独立事务。
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> {
                if (success) { mapper.complete(task.getId()); }
                else { mapper.defer(task.getId()); }
            });
            if (!success && publishRetry) {
                producer.send(MqTopics.CACHE_RETRY,
                        JSONUtil.toJsonStr(Collections.singletonMap("taskId", task.getId())));
            }
        } catch (Exception e) {
            // 不把提交后的补偿故障抛给调用方，避免客户端误以为报名失败而重复提交。
            log.error("Cache invalidation remains pending. taskId={}", task.getId(), e);
        }
    }

    public void retry(String taskId) {
        CacheInvalidationTask task = mapper.find(taskId);
        // MQ 消费失败由数据库定时任务继续重试，不立即重新投递造成消息风暴。
        if (task != null) { process(task, false); }
    }

    @Scheduled(initialDelayString = "${app.cache-retry.interval-ms:30000}",
            fixedDelayString = "${app.cache-retry.interval-ms:30000}")
    public void retryPending() {
        for (CacheInvalidationTask task : mapper.findDue()) { tryProcess(task); }
    }
}
