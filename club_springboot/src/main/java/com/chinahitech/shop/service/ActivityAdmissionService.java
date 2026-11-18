package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.exception.ActivityRegistrationConflict;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mapper.IndividualActivityMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 所有报名入口共用名额校验。正常路径由 Lua 原子完成查重、人数判断和预占；
 * 只有 Redis 不可用时才走数据库行锁，并用本地短窗口限流保护数据库。
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public class ActivityAdmissionService {
    private static final DefaultRedisScript<Long> RESERVE = script("activity_apply.lua");
    static final DefaultRedisScript<Long> RELEASE = script("activity_apply_rollback.lua");
    private final ConcurrentHashMap<Integer, AtomicLong> fallbackLimit = new ConcurrentHashMap<>();
    @Value("${app.activity.redis-fallback.max-requests-per-second:20}")
    private long fallbackMaxRps = 20;
    private java.time.Clock fallbackClock = java.time.Clock.systemUTC();
    @Resource private ActivityAdmissionRecovery recovery;
    @Resource private ActivityAdmissionOutboxService outboxService;
    @Value("${app.activity.async-write-enabled:false}") private boolean asyncWriteEnabled;
    @Value("${app.kafka.enabled:false}") private boolean kafkaEnabled;
    @Value("${app.kafka.listener.auto-startup:false}") private boolean admissionListenerEnabled;
    @Resource private ActivityMapper activityMapper;
    @Resource private IndividualActivityMapper individualActivityMapper;
    @Resource private StringRedisTemplate redis;
    @Resource private CacheInvalidationService invalidation;

    public void register(int activityId, String studentId, String position, int status) {
        requireStudent(studentId);
        Activity activity = activityMapper.getActivityById(activityId);
        if (activity == null) { throw new BusinessException("ACTIVITY_NOT_EXIST", "活动不存在"); }
        String token = UUID.randomUUID().toString();
        boolean synchronizedTransaction = TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizedTransaction) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int completionStatus) {
                    if (completionStatus == STATUS_ROLLED_BACK) {
                        release(activityId, studentId, token);
                    } else if (completionStatus == STATUS_UNKNOWN) {
                        // 提交结果不明时保留预占，供发件箱或消费回执核对。
                        log.warn("报名事务结果未知，activityId={}, reservationToken={}", activityId, token);
                    }
                }
            });
        }
        Long result = null;
        try {
            result = redis.execute(RESERVE,
                    reservationKeys(activityId),
                    studentId, String.valueOf(activity.getNumber()), "86400", token);
            if (Long.valueOf(2).equals(result)) { throw new BusinessException("ACTIVITY_FULL", "活动报名人数已满"); }
            if (Long.valueOf(3).equals(result)) { throw new BusinessException("APPLY_REPEAT", "你已经报名过该活动"); }
            // 兼容旧预占脚本的“计数尚未初始化”返回码。
            if (!Long.valueOf(1).equals(result) && !Long.valueOf(4).equals(result)) {
                throw new IllegalStateException("Unexpected activity reservation result: " + result);
            }
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            throttleFallback(activityId, e);
        }
        // 已确认的预占才可异步受理，容量始终在数据库行锁下检查。
        final boolean redisReserved = Long.valueOf(1L).equals(result);
        try {
            activity = lockActivity(activityId);
            if (redisReserved) requireCapacity(activity);
            if (individualActivityMapper.checkExist(studentId, activityId) != null) {
                throw new BusinessException("APPLY_REPEAT", "你已经报名过该活动");
            }
            if (!redisReserved) requireCapacity(activity);
            // 发送端和监听器都启用时才返回异步受理，否则直接落库。
            if (redisReserved && asyncWriteEnabled && kafkaEnabled && admissionListenerEnabled) {
                outboxService.enqueue(activityId, studentId, position, status, token);
                return;
            }
            IndividualActivity application = new IndividualActivity();
            application.setActivityId(activityId);
            application.setStudentId(studentId);
            application.setPosition(position);
            application.setStatus(status);
            individualActivityMapper.insert(application);
            // 与报名记录同库提交，补偿轮询器只能看到已提交的任务。
            recovery.recordSynchronousSuccess(activityId, studentId, token);
            evictQueries();
        } catch (RuntimeException e) {
            if (!synchronizedTransaction) { release(activityId, studentId, token); }
            if (ActivityRegistrationConflict.matches(e)) {
                // 异常继续向外传播以回滚当前事务，既有同步回调负责撤销本次 Redis 预占。
                throw new BusinessException("APPLY_REPEAT", "你已经报名过该活动", e);
            }
            throw e;
        }
    }

    /** 所有增加/释放名额的路径都锁同一活动行，不能只锁报名记录。 */
    public Activity lockActivity(int activityId) {
        if (activityId <= 0) { throw new BusinessException("PARAM_ERROR", "活动ID不合法"); }
        Activity activity = activityMapper.getActivityForUpdate(activityId);
        if (activity == null) { throw new BusinessException("ACTIVITY_NOT_EXIST", "活动不存在"); }
        return activity;
    }

    public void changeStatus(int activityId, String studentId, int status) {
        Activity activity = lockActivity(activityId);
        requireStudent(studentId);
        IndividualActivity existing = individualActivityMapper.checkExist(studentId, activityId);
        if (existing == null) { throw new BusinessException("NOT_FOUND", "未找到活动成员记录"); }
        if (existing.getStatus() == -1 && status != -1) { requireCapacity(activity); }
        individualActivityMapper.updateStatus(activityId, studentId, status);
        evictQueries();
    }

    public void remove(int activityId, String studentId) {
        lockActivity(activityId);
        requireStudent(studentId);
        if (individualActivityMapper.delete(activityId, studentId) <= 0) {
            throw new BusinessException("NOT_FOUND", "未找到活动成员记录");
        }
        evictQueries();
    }

    private int requireCapacity(Activity activity) {
        int count = activityMapper.countActiveApplications(activity.getId());
        if (activity.getNumber() > 0 && count >= activity.getNumber()) {
            throw new BusinessException("ACTIVITY_FULL", "活动报名人数已满");
        }
        return count;
    }

    private void requireStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BusinessException("PARAM_ERROR", "学生ID不能为空");
        }
    }

    private void release(int id, String studentId, String token) {
        try {
            Long result = redis.execute(RELEASE, reservationKeys(id), studentId, token);
            if (result == null) throw new IllegalStateException("Redis 未确认预占释放");
        } catch (RuntimeException failure) {
            try {
                // 回滚后的任务需要独立事务，否则会与原报名一起被回滚。
                recovery.recordRollback(id, studentId, token);
            } catch (RuntimeException persistenceFailure) {
                log.error("无法持久化报名预占补偿，activityId={}, token={}", id, token, persistenceFailure);
            }
        }
    }

    /** Kafka 消费者使用的幂等落库入口。 */
    public void consumeAdmission(int activityId, String studentId, String position, int status) {
        Activity activity = lockActivity(activityId);
        if (individualActivityMapper.checkExist(studentId, activityId) == null) {
            requireCapacity(activity);
            IndividualActivity application = new IndividualActivity();
            application.setActivityId(activityId); application.setStudentId(studentId);
            application.setPosition(position); application.setStatus(status);
            try {
                individualActivityMapper.insert(application);
                evictQueries();
            } catch (RuntimeException e) {
                // 查询后仍可能被其他写入口抢先插入；仅同一业务键已存在时视作幂等重放。
                if (!ActivityRegistrationConflict.matches(e)
                        || individualActivityMapper.checkExist(studentId, activityId) == null) {
                    throw e;
                }
            }
        }
    }

    private void evictQueries() {
        invalidation.evict("activity:all:*", "activity:page:*", "activity:detail:*",
                "activity:top", "activity:managed:*", "individualActivity:*");
    }

    static java.util.List<String> reservationKeys(int id) {
        return java.util.List.of("activity:apply:pending-expiry:{" + id + "}", "activity:apply:pending-tokens:{" + id + "}");
    }

    /** Redis 故障时只允许少量请求进入行锁，避免故障放大成数据库雪崩。 */
    private void throttleFallback(int activityId, RuntimeException cause) {
        long now = fallbackClock.millis();
        AtomicLong state = fallbackLimit.computeIfAbsent(activityId, k -> new AtomicLong(0L));
        long interval = Math.max(1L, 1000L / Math.max(1L, fallbackMaxRps));
        // 被拒绝的请求不能刷新窗口，否则持续流量会让数据库降级通道一直无法恢复。
        long previous;
        do {
            previous = state.get();
            if (now - previous < interval) {
                throw new BusinessException("SYSTEM_BUSY", "缓存服务暂不可用，请稍后重试");
            }
        } while (!state.compareAndSet(previous, now));
        log.warn("报名预占降级到数据库，activityId={}, causeType={}", activityId, cause.getClass().getSimpleName());
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
