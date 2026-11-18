package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.service.ActivityService;
import com.chinahitech.shop.utils.CacheData;
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.RedisUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 活动业务实现，处理活动查询缓存、报名防重复提交、活动审核和 MQ 通知。
 */
@Service
@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class ActivityServiceImpl implements ActivityService {

    private static final long ACTIVITY_DETAIL_LOGICAL_TTL_MINUTES = 10L;
    private static final long ACTIVITY_DETAIL_NULL_TTL_SECONDS = 120L;
    private static final long ACTIVITY_DETAIL_REBUILD_LOCK_SECONDS = 10L;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(2);

    @Resource
    private com.chinahitech.shop.service.ActivityAdmissionService admission;
    @Resource
    private com.chinahitech.shop.service.CacheInvalidationService invalidation;


    @Resource
    private ActivityMapper activityMapper;



    @Override
    public List<Activity> query(String searchInfo) {
        return activityMapper.query(searchInfo);
    }

    @Override
    public List<Activity> queryCached(String searchInfo) {
        String key = "activity:all:" + (searchInfo == null ? "" : searchInfo);
        List<Activity> activities = (List<Activity>) RedisUtils.getBusinessCache(key);
        if (activities == null) {
            activities = query(searchInfo);
            RedisUtils.setBusinessCache(key, activities, 300);
        }
        return activities;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Activity getActivityByNameAndGroupName(String activityName, String groupName) {
        return activityMapper.getActivityByNameAndGroupName(activityName, groupName);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Activity getCachedActivityDetail(String activityName, String groupName) {
        String key = activityDetailKey(groupName, activityName);
        Object cached = RedisUtils.getBusinessCache(key);

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
        /**
         * 保存业务数据或本地状态。
         */
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
        List<Activity> activities = (List<Activity>) RedisUtils.getBusinessCache(key);
        if (activities == null) {
            activities = queryTop();
            RedisUtils.setBusinessCache(key, activities, 120);
        }
        return activities;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoin(Integer activityId, Integer studentId) {
        if (activityId == null || studentId == null) {
            throw new BusinessException("PARAM_ERROR", "活动和学生编号不能为空");
        }
        admission.register(activityId, String.valueOf(studentId), "成员", 0);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Activity> getMyJoinedActivities(Integer studentId) {
        return activityMapper.getMyJoinedActivities(studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Activity> getActivityByGroupName(String groupName) {
        return activityMapper.getActivityByGroupName(groupName);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Map<String, Object>> getActivityApplicants(Integer activityId) {
        return com.chinahitech.shop.utils.ContactPrivacy.applicants(activityMapper.getActivityApplicants(activityId));
    }

    @Override
    public void auditApply(Integer id, Integer status) {
        if (id == null || status == null) {
            throw new BusinessException("PARAM_ERROR", "申请编号和状态不能为空");
        }
        com.chinahitech.shop.bean.IndividualActivity application = activityMapper.getApplicationById(id);
        if (application == null) { throw new BusinessException("NOT_FOUND", "申请不存在"); }
        admission.changeStatus(application.getActivityId(), application.getStudentId(), status);
    }

    @Override
    public void insert(Activity activity) {
        validateActivityForSave(activity, false);
        activityMapper.insert(activity);
        // 数据库触发器在同一事务记录搜索任务；不在主业务事务内调用 ES。
        evictActivityCache();
    }

    @Override
    public PageResult<Activity> queryPageCached(String searchInfo, Integer pageNum, Integer pageSize) {
        int offset = PageUtils.offset(pageNum, pageSize);
        int limit = PageUtils.limit(pageSize);
        String normalizedSearch = searchInfo == null ? "" : searchInfo.trim();
        String key = "activity:page:" + normalizedSearch + ':' + offset + ':' + limit;
        PageResult<Activity> page = (PageResult<Activity>) RedisUtils.getBusinessCache(key);
        if (page == null) {
            List<Activity> records = activityMapper.queryPage(searchInfo, offset, limit);
            long total = activityMapper.countQuery(searchInfo);
            page = PageUtils.pageResult(records, total, pageNum, pageSize);
            RedisUtils.setBusinessCache(key, page, 300);
        }
        return page;
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updateDescription(String groupName, String activityName, String description, String attachment, String image) {
        activityMapper.updateDescription(groupName, activityName, description, attachment, image);
        evictActivityCache();
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updateAttachment(String name, String attachment) {
        activityMapper.updateAttachment(name, attachment);
        evictActivityCache();
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updateImage(String name, String image) {
        activityMapper.updateImage(name, image);
        evictActivityCache();
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Activity getActivityById(Integer id) {
        return activityMapper.getActivityById(id);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public String getAttachment(Integer id) {
        Activity activity = getActivityById(id);
        if (activity == null) {
            throw new BusinessException("ACTIVITY_NOT_EXIST", "\u6d3b\u52a8\u4e0d\u5b58\u5728");
        }
        return activity.getAttachment();
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Activity> getAllApp(String searchinfo) {
        return activityMapper.getAllApp(searchinfo);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Activity getAppByName(String groupname) {
        return activityMapper.getAppByName(groupname);
    }

    @Override
    public void confirmApplication(Integer activityId) {
        activityMapper.confirmApplication(activityId);
        evictActivityCache();
    }

    @Override
    public void denyApplication(Integer activityId) {
        activityMapper.denyApplication(activityId);
        evictActivityCache();
    }

    @Override
    public void modifyInfo(Activity activity) {
        validateActivityForSave(activity, true);
        activityMapper.modifyInfo(activity);
        evictActivityCache();
    }

    /**
     * 活动发布和修改都必须带上能定位业务归属的核心字段，避免空对象被误当成成功保存。
     */
    private void validateActivityForSave(Activity activity, boolean requireId) {
        if (activity == null) {
            throw new BusinessException("活动信息不能为空");
        }
        if (requireId && activity.getId() <= 0) {
            throw new BusinessException("活动编号不能为空");
        }
        if (isBlank(activity.getName())) {
            throw new BusinessException("活动名称不能为空");
        }
        if (isBlank(activity.getGroupName())) {
            throw new BusinessException("所属社团不能为空");
        }
    }

    /**
     * 统一判断字符串是否为空，避免每个校验点重复编写 null 和 trim 逻辑。
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
                /**
                 * 保存业务数据或本地状态。
                 */
                saveActivityDetailCache(key, fresh);
            } finally {
                RedisUtils.del(lockKey);
            }
        });
    }

    /**
     * 保存业务数据或本地状态。
     */
    private void saveActivityDetailCache(String key, Activity activity) {
        if (activity == null) {
            RedisUtils.setBusinessCache(key, RedisUtils.NULL_VALUE, ACTIVITY_DETAIL_NULL_TTL_SECONDS);
            return;
        }
        CacheData<Activity> cacheData = new CacheData<>(
                activity,
                LocalDateTime.now().plusMinutes(ACTIVITY_DETAIL_LOGICAL_TTL_MINUTES)
        );
        RedisUtils.setBusinessCache(key, cacheData, 1200);
    }

    private void evictActivityCache() {
        // 显式列举查询缓存，兼容旧键且不会误删报名占位。
        invalidation.evict("activity:all:*", "activity:page:*", "activity:detail:*",
                "activity:top", "activity:managed:*", "individualActivity:*");
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Activity> getActivityByGroupNamePage(String groupName, String searchInfo, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = activityMapper.getActivityByGroupNamePage(groupName, searchInfo, offset, limit);
        long total = activityMapper.getActivityByGroupNameCount(groupName, searchInfo);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Activity> getAllAppPage(String searchInfo, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = activityMapper.getAllAppPage(searchInfo, offset, limit);
        long total = activityMapper.getAllAppCount(searchInfo);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Activity> getMyJoinedActivitiesPage(int studentId, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = activityMapper.getMyJoinedActivitiesPage(studentId, offset, limit);
        long total = activityMapper.getMyJoinedActivitiesCount(studentId);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<java.util.Map<String, Object>> getActivityApplicantsPage(int activityId, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = com.chinahitech.shop.utils.ContactPrivacy.applicants(activityMapper.getActivityApplicantsPage(activityId, offset, limit));
        long total = activityMapper.getActivityApplicantsCount(activityId);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }
}
