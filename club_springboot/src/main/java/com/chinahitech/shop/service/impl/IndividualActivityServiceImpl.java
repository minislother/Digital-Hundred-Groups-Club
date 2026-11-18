package com.chinahitech.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.bean.IndividualActivity;
import com.chinahitech.shop.bean.notAddedToDatabase.ActivityNum;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.IndividualActivityMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.service.IndividualActivityService;
import com.chinahitech.shop.utils.RedisUtils;
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动成员关系实现，处理学生报名、管理员审核/维护活动成员、缓存失效和 MQ 通知。
 * 管理端写操作会先校验当前管理员是否有权管理目标活动。
 */
@Service
@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class IndividualActivityServiceImpl implements IndividualActivityService {

    @Autowired
    private IndividualActivityMapper individualActivityMapper;

    @Autowired
    private KafkaProducer kafkaProducer;
    @Autowired
    private com.chinahitech.shop.service.ActivityAdmissionService admission;
    @Autowired
    private com.chinahitech.shop.service.CacheInvalidationService invalidation;

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getActivityByStuId(String studentId) {
        return individualActivityMapper.getActivityByStuId(studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getActivityByStuIdCached(String studentId) {
        requireStudentId(studentId);
        String key = studentKey(studentId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getActivityByStuId(studentId);
            RedisUtils.setBusinessCache(key, list, 300);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinActivity(String studentId, int activityId) {
        admission.register(activityId, studentId, "成员", 0);
    }

    @Override
    public void joinActivityAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        joinActivity(studentId, activityId);
        sendEvent("join_activity_apply", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getActivityByActivityId(int activityId) {
        return individualActivityMapper.getActivityByActivityId(activityId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getActivityByActivityIdCached(int activityId) {
        requireActivityId(activityId);
        String key = studentsKey(activityId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getActivityByActivityId(activityId);
            RedisUtils.setBusinessCache(key, list, 120);
        }
        return list;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getApplyByActivityId(int activityId) {
        return individualActivityMapper.getApplyByActivityId(activityId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualActivity> getApplyByActivityIdCached(int activityId) {
        requireActivityId(activityId);
        String key = applyKey(activityId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getApplyByActivityId(activityId);
            RedisUtils.setBusinessCache(key, list, 60);
        }
        return list;
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addActivityStudent(String studentId, int activityId, String position, boolean approved) {
        admission.register(activityId, studentId, position, approved ? 1 : 0);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addActivityStudentAndNotify(int activityId, String studentId, String position) {
        requireActivityStudent(activityId, studentId);
        /**
         * 新增业务数据或追加界面状态。
         */
        addActivityStudent(studentId, activityId, position, true);
        sendEvent("add_activity_student", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addActivityStudentAndNotify(int activityId, String studentId, String position, String managerId) {
        assertManagedActivity(activityId, managerId);
        /**
         * 新增业务数据或追加界面状态。
         */
        addActivityStudentAndNotify(activityId, studentId, position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyActivityStudent(int activityId, String studentId, String position) {
        ensureUpdated(individualActivityMapper.updatePosition(activityId, studentId, position));
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void modifyActivityStudentAndNotify(int activityId, String studentId, String position) {
        requireActivityStudent(activityId, studentId);
        modifyActivityStudent(activityId, studentId, position);
        sendEvent("modify_activity_student", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void modifyActivityStudentAndNotify(int activityId, String studentId, String position, String managerId) {
        assertManagedActivity(activityId, managerId);
        modifyActivityStudentAndNotify(activityId, studentId, position);
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivityStudent(int activityId, String studentId) {
        admission.remove(activityId, studentId);
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    public void deleteActivityStudentAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        /**
         * 删除指定业务数据。
         */
        deleteActivityStudent(activityId, studentId);
        sendEvent("delete_activity_student", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    public void deleteActivityStudentAndNotify(int activityId, String studentId, String managerId) {
        assertManagedActivity(activityId, managerId);
        /**
         * 删除指定业务数据。
         */
        deleteActivityStudentAndNotify(activityId, studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Activity> getAllManagedActivities(String managerId) {
        return individualActivityMapper.getAllManagedActivities(managerId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Activity> getAllManagedActivitiesCached(String managerId) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        String key = "individualActivity:manager:" + managerId;
        List<Activity> list = (List<Activity>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getAllManagedActivities(managerId);
            RedisUtils.setBusinessCache(key, list, 300);
        }
        return list;
    }

    @Override
    public PageResult<Activity> getAllManagedActivitiesPageCached(String managerId,
                                                                  Integer pageNum,
                                                                  Integer pageSize) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        int offset = PageUtils.offset(pageNum, pageSize);
        int limit = PageUtils.limit(pageSize);
        // activity 前缀会被活动资料变更时的统一缓存失效逻辑清理。
        String key = "activity:managed:" + managerId + ':' + offset + ':' + limit;
        PageResult<Activity> page = (PageResult<Activity>) RedisUtils.getBusinessCache(key);
        if (page == null) {
            List<Activity> records = individualActivityMapper
                    .getAllManagedActivitiesPage(managerId, offset, limit);
            long total = individualActivityMapper.countAllManagedActivities(managerId);
            page = PageUtils.pageResult(records, total, pageNum, pageSize);
            RedisUtils.setBusinessCache(key, page, 300);
        }
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmApplication(int activityId, String studentId) {
        admission.changeStatus(activityId, studentId, 1);
    }

    @Override
    public void confirmApplicationAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        confirmApplication(activityId, studentId);
        sendEvent("accept_apply", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void confirmApplicationAndNotify(int activityId, String studentId, String managerId) {
        assertManagedActivity(activityId, managerId);
        confirmApplicationAndNotify(activityId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void denyApplication(int activityId, String studentId) {
        admission.changeStatus(activityId, studentId, -1);
    }

    @Override
    public void denyApplicationAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        denyApplication(activityId, studentId);
        sendEvent("reject_apply", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void denyApplicationAndNotify(int activityId, String studentId, String managerId) {
        assertManagedActivity(activityId, managerId);
        denyApplicationAndNotify(activityId, studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<ActivityNum> getActivityMembers() {
        return individualActivityMapper.getActivityMembers();
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<ActivityNum> getActivityMembersCached() {
        String key = "individualActivity:top5:members";
        List<ActivityNum> list = (List<ActivityNum>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getActivityMembers();
            RedisUtils.setBusinessCache(key, list, 600);
        }
        return list;
    }

    private void requireActivityStudent(int activityId, String studentId) {
        requireActivityId(activityId);
        requireStudentId(studentId);
    }

    private void assertManagedActivity(int activityId, String managerId) {
        requireActivityId(activityId);
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        // create_user 和 organizer 都视为该活动的合法管理员。
        if (individualActivityMapper.countManagedActivity(activityId, managerId) <= 0) {
            throw new BusinessException("FORBIDDEN", "\u65e0\u6743\u64cd\u4f5c\u8be5\u6d3b\u52a8");
        }
    }

    /**
     * 校验活动 ID 是否有效，无效时抛出业务异常。
     */
    private void requireActivityId(int activityId) {
        if (activityId <= 0) {
            throw new BusinessException("PARAM_ERROR", "\u6d3b\u52a8ID\u4e0d\u5408\u6cd5");
        }
    }

    private void requireStudentId(String studentId) {
        requireText(studentId, "\u5b66\u751fID\u4e0d\u80fd\u4e3a\u7a7a");
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("PARAM_ERROR", message);
        }
    }

    private void ensureUpdated(int rows) {
        // update/delete 影响 0 行表示目标申请或成员关系不存在，不能返回业务成功。
        if (rows <= 0) {
            throw new BusinessException("NOT_FOUND", "\u672a\u627e\u5230\u53ef\u66f4\u65b0\u7684\u6d3b\u52a8\u6210\u5458\u8bb0\u5f55");
        }
    }

    /**
     * 发送业务事件消息，用于异步同步或缓存刷新。
     */
    private void sendEvent(String type, int activityId, String studentId, Map<String, Object> extra) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("activityId", activityId);
        msg.put("studentId", studentId);
        if (extra != null) {
            msg.putAll(extra);
        }
        kafkaProducer.send(MqTopics.INDIVIDUAL_ACTIVITY, JSONUtil.toJsonStr(msg));
    }

    private void evictActivityStudentCache(int activityId, String studentId) {
        invalidation.evict(studentsKey(activityId), applyKey(activityId), studentKey(studentId),
                "individualActivity:top5:members");
    }

    private String studentKey(String studentId) {
        return "individualActivity:student:" + studentId;
    }

    private String studentsKey(int activityId) {
        return "individualActivity:students:" + activityId;
    }

    private String applyKey(int activityId) {
        return "individualActivity:apply:" + activityId;
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<IndividualActivity> getActivityByStuIdPage(String studentId, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = individualActivityMapper.getActivityByStuIdPage(studentId, offset, limit);
        long total = individualActivityMapper.getActivityByStuIdCount(studentId);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<IndividualActivity> getActivityByActivityIdPage(int activityId, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = individualActivityMapper.getActivityByActivityIdPage(activityId, offset, limit);
        long total = individualActivityMapper.getActivityByActivityIdCount(activityId);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<IndividualActivity> getApplyByActivityIdPage(int activityId, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = individualActivityMapper.getApplyByActivityIdPage(activityId, offset, limit);
        long total = individualActivityMapper.getApplyByActivityIdCount(activityId);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 下一页多取一条判断 hasNext，不执行总数统计。 */
    @Override
    public com.chinahitech.shop.utils.Result studentSlice(String studentId, Long beforeId, Integer pageSize) {
        if (beforeId != null && beforeId <= 0) throw new com.chinahitech.shop.exception.BusinessException("PARAM_ERROR", "游标必须为正数");
        int size = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var rows = individualActivityMapper.studentSlice(studentId, beforeId, size + 1);
        boolean hasNext = rows.size() > size;
        var items = rows.subList(0, Math.min(size, rows.size()));
        return com.chinahitech.shop.utils.Result.ok().data("items", items).data("hasNext", hasNext)
                .data("nextCursor", hasNext ? items.getLast().getId() : null);
    }

    /** 高增长成员或申请列表按不可变关系 ID 连续翻页，保留管理员资源范围校验。 */
    @Override
    public com.chinahitech.shop.utils.Result memberSlice(int resourceId, int status, Long beforeId, Integer pageSize, String managerId) {
        assertManagedActivity(resourceId, managerId);
        if ((status != 0 && status != 1) || (beforeId != null && beforeId <= 0)) {
            throw new com.chinahitech.shop.exception.BusinessException("PARAM_ERROR", "成员游标或状态不合法");
        }
        int size = PageUtils.limit(pageSize);
        var rows = individualActivityMapper.memberSlice(resourceId, status, beforeId, size + 1);
        boolean hasNext = rows.size() > size;
        var items = rows.subList(0, Math.min(size, rows.size()));
        return com.chinahitech.shop.utils.Result.ok().data("items", items).data("hasNext", hasNext)
                .data("nextCursor", hasNext ? items.getLast().getId() : null);
    }
}
