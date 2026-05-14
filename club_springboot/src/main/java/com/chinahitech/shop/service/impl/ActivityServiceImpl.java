package com.chinahitech.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.utils.CacheData;
import com.chinahitech.shop.utils.RedisUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final String ACTIVITY_CACHE_PATTERN = "activity:*";
    private static final long APPLY_GUARD_TTL_SECONDS = 24 * 60 * 60L;
    private static final long ACTIVITY_DETAIL_LOGICAL_TTL_MINUTES = 10L;
    private static final long ACTIVITY_DETAIL_NULL_TTL_SECONDS = 2 * 60L;
    private static final long ACTIVITY_DETAIL_REBUILD_LOCK_SECONDS = 10L;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(2);

    private static final DefaultRedisScript<Long> APPLY_ACTIVITY_SCRIPT;

    static {
        APPLY_ACTIVITY_SCRIPT = new DefaultRedisScript<>();
        APPLY_ACTIVITY_SCRIPT.setLocation(new ClassPathResource("activity_apply.lua"));
        APPLY_ACTIVITY_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KafkaProducer kafkaProducer;

    @Override
    public List<Activity> query(String searchInfo) {
        return activityMapper.query(searchInfo);
    }

    @Override
    public List<Activity> queryCached(String searchInfo) {
        String key = "activity:all:" + (searchInfo == null ? "" : searchInfo);
        List<Activity> activities = (List<Activity>) RedisUtils.get(key);
        if (activities == null) {
            activities = query(searchInfo);
            RedisUtils.set(key, activities, 300);
        }
        return activities;
    }

    @Override
    public Activity getActivityByNameAndGroupName(String activityName, String groupName) {
        return activityMapper.getActivityByNameAndGroupName(activityName, groupName);
    }

    @Override
    public Activity getCachedActivityDetail(String activityName, String groupName) {
        String key = activityDetailKey(groupName, activityName);
        Object cached = RedisUtils.get(key);

        if (RedisUtils.NULL_VALUE.equals(cached)) {
            return null;
        }
        if (cached instanceof CacheData) {
            CacheData<?> cacheData = (CacheData<?>) cached;
            Activity activity = (Activity) cacheData.getData();
            if (cacheData.getExpireTime() != null && cacheData.getExpireTime().isAfter(LocalDateTime.now())) {
                return activity;
            }
            tryRebuildActivityDetailCache(key, activityName, groupName);
            return activity;
        }
        if (cached instanceof Activity) {
            return (Activity) cached;
        }

        Activity activity = activityMapper.getActivityByNameAndGroupName(activityName, groupName);
        saveActivityDetailCache(key, activity);
        return activity;
    }

    @Override
    public List<Activity> queryTop() {
        return activityMapper.queryTop();
    }

    @Override
    public List<Activity> queryTopCached() {
        String key = "activity:top";
        List<Activity> activities = (List<Activity>) RedisUtils.get(key);
        if (activities == null) {
            activities = queryTop();
            RedisUtils.set(key, activities, 120);
        }
        return activities;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoin(Integer activityId, Integer studentId) {
        Activity activity = activityMapper.getActivityById(activityId);
        if (activity == null) {
            throw new BusinessException("ACTIVITY_NOT_EXIST", "\u6d3b\u52a8\u4e0d\u5b58\u5728");
        }

        Map<String, Object> check = activityMapper.checkApply(activityId, studentId);
        if (check != null) {
            throw new BusinessException("APPLY_REPEAT", "\u4f60\u5df2\u7ecf\u62a5\u540d\u8fc7\u8be5\u6d3b\u52a8");
        }

        int dbCount = activityMapper.countActiveApplications(activityId);
        if (activity.getNumber() > 0 && dbCount >= activity.getNumber()) {
            throw new BusinessException("ACTIVITY_FULL", "\u6d3b\u52a8\u62a5\u540d\u4eba\u6570\u5df2\u6ee1");
        }

        Long guardResult = tryApplyGuard(activityId, studentId, activity.getNumber(), dbCount);
        if (guardResult != null && guardResult == 1L) {
            throw new BusinessException("APPLY_REPEAT", "\u4f60\u5df2\u7ecf\u62a5\u540d\u8fc7\u8be5\u6d3b\u52a8");
        }
        if (guardResult != null && guardResult == 2L) {
            throw new BusinessException("ACTIVITY_FULL", "\u6d3b\u52a8\u62a5\u540d\u4eba\u6570\u5df2\u6ee1");
        }

        try {
            activityMapper.applyJoin(activityId, studentId, 0);
            evictActivityCache();
        } catch (RuntimeException e) {
            rollbackApplyGuard(activityId, studentId, guardResult);
            throw e;
        }
    }

    @Override
    public List<Activity> getMyJoinedActivities(Integer studentId) {
        return activityMapper.getMyJoinedActivities(studentId);
    }

    @Override
    public List<Activity> getActivityByGroupName(String groupName) {
        return activityMapper.getActivityByGroupName(groupName);
    }

    @Override
    public List<Map<String, Object>> getActivityApplicants(Integer activityId) {
        return activityMapper.getActivityApplicants(activityId);
    }

    @Override
    public void auditApply(Integer id, Integer status) {
        activityMapper.auditApply(id, status);
        evictActivityCache();
    }

    @Override
    public void insert(Activity activity) {
        activityMapper.insert(activity);
        evictActivityCache();
    }

    @Override
    public void updateDescription(String groupName, String activityName, String description, String attachment, String image) {
        activityMapper.updateDescription(groupName, activityName, description, attachment, image);
        evictActivityCache();
    }

    @Override
    public void updateAttachment(String name, String attachment) {
        activityMapper.updateAttachment(name, attachment);
        evictActivityCache();
    }

    @Override
    public void updateImage(String name, String image) {
        activityMapper.updateImage(name, image);
        evictActivityCache();
    }

    @Override
    public Activity getActivityById(Integer id) {
        return activityMapper.getActivityById(id);
    }

    @Override
    public String getAttachment(Integer id) {
        Activity activity = getActivityById(id);
        if (activity == null) {
            throw new BusinessException("ACTIVITY_NOT_EXIST", "\u6d3b\u52a8\u4e0d\u5b58\u5728");
        }
        return activity.getAttachment();
    }

    @Override
    public List<Activity> getAllApp(String searchinfo) {
        return activityMapper.getAllApp(searchinfo);
    }

    @Override
    public Activity getAppByName(String groupname) {
        return activityMapper.getAppByName(groupname);
    }

    @Override
    public void confirmApplication(Integer activityId) {
        activityMapper.confirmApplication(activityId);
        clearActivityApplyGuard(activityId);
        evictActivityCache();
    }

    @Override
    public void denyApplication(Integer activityId) {
        activityMapper.denyApplication(activityId);
        clearActivityApplyGuard(activityId);
        evictActivityCache();
    }

    @Override
    public void modifyInfo(Activity activity) {
        activityMapper.modifyInfo(activity);
        clearActivityApplyGuard(activity.getId());
        evictActivityCache();
    }

    private Long tryApplyGuard(Integer activityId, Integer studentId, int limit, int dbCount) {
        try {
            return stringRedisTemplate.execute(
                    APPLY_ACTIVITY_SCRIPT,
                    java.util.Arrays.asList(applyStudentsKey(activityId), applyCountKey(activityId)),
                    String.valueOf(studentId),
                    String.valueOf(limit),
                    String.valueOf(dbCount),
                    String.valueOf(APPLY_GUARD_TTL_SECONDS)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void rollbackApplyGuard(Integer activityId, Integer studentId, Long guardResult) {
        if (guardResult == null || guardResult != 0L) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().remove(applyStudentsKey(activityId), String.valueOf(studentId));
            stringRedisTemplate.opsForValue().decrement(applyCountKey(activityId));
        } catch (Exception ignored) {
        }
    }

    private void clearActivityApplyGuard(Integer activityId) {
        if (activityId == null) {
            return;
        }
        RedisUtils.del(applyStudentsKey(activityId));
        RedisUtils.del(applyCountKey(activityId));
    }

    private String applyStudentsKey(Integer activityId) {
        return "activity:apply:students:" + activityId;
    }

    private String applyCountKey(Integer activityId) {
        return "activity:apply:count:" + activityId;
    }

    private String activityDetailKey(String groupName, String activityName) {
        return "activity:detail:" + groupName + ":" + activityName;
    }

    private String activityDetailLockKey(String groupName, String activityName) {
        return "lock:activity:detail:" + groupName + ":" + activityName;
    }

    private void tryRebuildActivityDetailCache(String key, String activityName, String groupName) {
        String lockKey = activityDetailLockKey(groupName, activityName);
        if (!RedisUtils.setIfAbsent(lockKey, "1", ACTIVITY_DETAIL_REBUILD_LOCK_SECONDS)) {
            return;
        }
        CACHE_REBUILD_EXECUTOR.execute(() -> {
            try {
                Activity fresh = activityMapper.getActivityByNameAndGroupName(activityName, groupName);
                saveActivityDetailCache(key, fresh);
            } finally {
                RedisUtils.del(lockKey);
            }
        });
    }

    private void saveActivityDetailCache(String key, Activity activity) {
        if (activity == null) {
            RedisUtils.set(key, RedisUtils.NULL_VALUE, ACTIVITY_DETAIL_NULL_TTL_SECONDS);
            return;
        }
        CacheData<Activity> cacheData = new CacheData<>(
                activity,
                LocalDateTime.now().plusMinutes(ACTIVITY_DETAIL_LOGICAL_TTL_MINUTES)
        );
        RedisUtils.set(key, cacheData);
    }

    private void evictActivityCache() {
        if (!RedisUtils.del(ACTIVITY_CACHE_PATTERN)) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "activity_cache_evict");
            msg.put("pattern", ACTIVITY_CACHE_PATTERN);
            kafkaProducer.send(MqTopics.ACTIVITY_CACHE, JSONUtil.toJsonStr(msg));
        }
    }
}
