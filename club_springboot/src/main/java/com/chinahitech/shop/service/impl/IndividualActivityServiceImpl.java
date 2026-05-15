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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人活动关系服务。
 *
 * <p>负责学生报名活动、管理员审批/维护活动成员、缓存失效和 MQ 通知。
 * 管理端写操作必须先校验当前管理员是否管理目标活动。</p>
 */
@Service
public class IndividualActivityServiceImpl implements IndividualActivityService {

    @Autowired
    private IndividualActivityMapper individualActivityMapper;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Override
    public List<IndividualActivity> getActivityByStuId(String studentId) {
        return individualActivityMapper.getActivityByStuId(studentId);
    }

    @Override
    public List<IndividualActivity> getActivityByStuIdCached(String studentId) {
        requireStudentId(studentId);
        String key = studentKey(studentId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.get(key);
        if (list == null) {
            list = getActivityByStuId(studentId);
            RedisUtils.set(key, list, 300);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinActivity(String studentId, int activityId) {
        IndividualActivity exist = individualActivityMapper.checkExist(studentId, activityId);
        if (exist != null) {
            throw new BusinessException("APPLY_EXIST", "\u5df2\u7533\u8bf7\u6216\u5df2\u52a0\u5165\u8be5\u6d3b\u52a8");
        }
        IndividualActivity ia = new IndividualActivity();
        ia.setStudentId(studentId);
        ia.setActivityId(activityId);
        ia.setStatus(0);
        ia.setPosition("\u6210\u5458");
        individualActivityMapper.insert(ia);
    }

    @Override
    public void joinActivityAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        joinActivity(studentId, activityId);
        sendEvent("join_activity_apply", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public List<IndividualActivity> getActivityByActivityId(int activityId) {
        return individualActivityMapper.getActivityByActivityId(activityId);
    }

    @Override
    public List<IndividualActivity> getActivityByActivityIdCached(int activityId) {
        requireActivityId(activityId);
        String key = studentsKey(activityId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.get(key);
        if (list == null) {
            list = getActivityByActivityId(activityId);
            RedisUtils.set(key, list, 120);
        }
        return list;
    }

    @Override
    public List<IndividualActivity> getApplyByActivityId(int activityId) {
        return individualActivityMapper.getApplyByActivityId(activityId);
    }

    @Override
    public List<IndividualActivity> getApplyByActivityIdCached(int activityId) {
        requireActivityId(activityId);
        String key = applyKey(activityId);
        List<IndividualActivity> list = (List<IndividualActivity>) RedisUtils.get(key);
        if (list == null) {
            list = getApplyByActivityId(activityId);
            RedisUtils.set(key, list, 60);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addActivityStudent(String studentId, int activityId, String position, boolean approved) {
        IndividualActivity ia = new IndividualActivity();
        ia.setStudentId(studentId);
        ia.setActivityId(activityId);
        ia.setPosition(position);
        ia.setStatus(approved ? 1 : 0);
        individualActivityMapper.insert(ia);
    }

    @Override
    public void addActivityStudentAndNotify(int activityId, String studentId, String position) {
        requireActivityStudent(activityId, studentId);
        addActivityStudent(studentId, activityId, position, true);
        sendEvent("add_activity_student", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void addActivityStudentAndNotify(int activityId, String studentId, String position, String managerId) {
        assertManagedActivity(activityId, managerId);
        addActivityStudentAndNotify(activityId, studentId, position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyActivityStudent(int activityId, String studentId, String position) {
        ensureUpdated(individualActivityMapper.updatePosition(activityId, studentId, position));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivityStudent(int activityId, String studentId) {
        ensureUpdated(individualActivityMapper.delete(activityId, studentId));
    }

    @Override
    public void deleteActivityStudentAndNotify(int activityId, String studentId) {
        requireActivityStudent(activityId, studentId);
        deleteActivityStudent(activityId, studentId);
        sendEvent("delete_activity_student", activityId, studentId, null);
        evictActivityStudentCache(activityId, studentId);
    }

    @Override
    public void deleteActivityStudentAndNotify(int activityId, String studentId, String managerId) {
        assertManagedActivity(activityId, managerId);
        deleteActivityStudentAndNotify(activityId, studentId);
    }

    @Override
    public List<Activity> getAllManagedActivities(String managerId) {
        return individualActivityMapper.getAllManagedActivities(managerId);
    }

    @Override
    public List<Activity> getAllManagedActivitiesCached(String managerId) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        String key = "individualActivity:manager:" + managerId;
        List<Activity> list = (List<Activity>) RedisUtils.get(key);
        if (list == null) {
            list = getAllManagedActivities(managerId);
            RedisUtils.set(key, list, 300);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmApplication(int activityId, String studentId) {
        ensureUpdated(individualActivityMapper.updateStatus(activityId, studentId, 1));
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
        ensureUpdated(individualActivityMapper.updateStatus(activityId, studentId, -1));
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

    @Override
    public List<ActivityNum> getActivityMembers() {
        return individualActivityMapper.getActivityMembers();
    }

    @Override
    public List<ActivityNum> getActivityMembersCached() {
        String key = "individualActivity:top5:members";
        List<ActivityNum> list = (List<ActivityNum>) RedisUtils.get(key);
        if (list == null) {
            list = getActivityMembers();
            RedisUtils.set(key, list, 600);
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
        // update/delete 影响 0 行通常表示目标申请或成员关系不存在，不能返回业务成功。
        if (rows <= 0) {
            throw new BusinessException("NOT_FOUND", "\u672a\u627e\u5230\u53ef\u66f4\u65b0\u7684\u6d3b\u52a8\u6210\u5458\u8bb0\u5f55");
        }
    }

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
        RedisUtils.del(studentsKey(activityId));
        RedisUtils.del(applyKey(activityId));
        RedisUtils.del(studentKey(studentId));
        RedisUtils.del("individualActivity:top5:members");
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
}
