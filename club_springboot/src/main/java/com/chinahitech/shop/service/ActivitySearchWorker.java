package com.chinahitech.shop.service;

import com.chinahitech.shop.mapper.ActivitySearchOutboxMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;

/** 持久化索引投影消费者；每次独立短事务领取一条，失败退避，重启后继续消费。 */
@Component
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
public class ActivitySearchWorker {
    private final ActivitySearchOutboxMapper outbox;
    private final ActivitySearchService search;
    private final TransactionTemplate transaction;
    private volatile boolean ready;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ActivitySearchWorker.class);

    public ActivitySearchWorker(ActivitySearchOutboxMapper outbox, ActivitySearchService search,
                                PlatformTransactionManager manager) {
        this.outbox = outbox;
        this.search = search;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // 开启功能却没有迁移必须显式失败，不能悄悄漏同步。ES 暂时离线不阻止主业务启动。
        if (outbox.triggerCount() != 3) throw new IllegalStateException("请先执行 activity-search-outbox.sql");
        outbox.status();
        ready = true;
        try { search.ensureIndex(); }
        catch (Exception e) { LOG.warn("ES 初始化暂未完成，后台将重试: {}", e.toString()); }
    }

    @Scheduled(fixedDelayString = "${app.elasticsearch.sync-delay-ms:2000}", initialDelay = 5000)
    public void drain() {
        if (!ready) return;
        try {
            // 控制每轮占用；单实例一个消费者，SKIP LOCKED 支持多个应用实例。
            for (int i = 0; i < 50 && consumeOne(); i++) { }
        } catch (RuntimeException e) { LOG.error("ES 同步队列读取失败", e); }
    }

    public boolean consumeOne() {
        Boolean consumed = transaction.execute(status -> {
            Map<String, Object> event = outbox.lockNext();
            if (event == null) return false;
            long id = ((Number) event.get("id")).longValue();
            int activityId = ((Number) event.get("activityId")).intValue();
            int attempts = ((Number) event.get("attempts")).intValue();
            try {
                search.synchronize(activityId, id);
                outbox.complete(id);
            } catch (Exception e) {
                String message = e.toString();
                outbox.retry(id, message.substring(0, Math.min(500, message.length())),
                        Math.min(300, 1 << Math.min(8, attempts + 1)));
                LOG.warn("ES 同步失败，保留任务重试: event={}, activity={}, reason={}", id, activityId, message);
                return false; // 集群故障时不继续打满 50 次超时。
            }
            return true;
        });
        return Boolean.TRUE.equals(consumed);
    }
}
