package com.chinahitech.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.bean.IndividualGroup;
import com.chinahitech.shop.bean.notAddedToDatabase.GroupNum;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.IndividualGroupMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.service.GroupService;
import com.chinahitech.shop.service.IndividualGroupService;
import com.chinahitech.shop.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndividualGroupServiceImpl implements IndividualGroupService {

    private static final long APPLY_GUARD_TTL_SECONDS = 24 * 60 * 60L;

    @Autowired
    private IndividualGroupMapper individualGroupMapper;

    @Autowired
    private GroupService groupService;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Override
    public List<IndividualGroup> getGroupByStuId(String studentId) {
        return individualGroupMapper.getGroupByStuId(studentId);
    }

    @Override
    public Map<String, Object> getStudentGroupsCached(String studentId) {
        requireStudentId(studentId);
        String key = studentKey(studentId);
        Map<String, Object> map = (Map<String, Object>) RedisUtils.get(key);
        if (map == null) {
            List<IndividualGroup> individualGroupList = getGroupByStuId(studentId);
            List<Group> groupList = new ArrayList<>();
            for (IndividualGroup individualGroup : individualGroupList) {
                groupList.add(groupService.getGroupById(individualGroup.getGroupId()));
            }

            map = new HashMap<>();
            map.put("stuItems", individualGroupList);
            map.put("groupItems", groupList);
            RedisUtils.set(key, map, 300);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoinGroup(int groupId, String studentId) {
        String guardKey = groupApplyGuardKey(groupId, studentId);
        if (!RedisUtils.setIfAbsent(guardKey, "1", APPLY_GUARD_TTL_SECONDS)) {
            throw new BusinessException("APPLY_EXIST", "\u5df2\u7533\u8bf7\u6216\u5df2\u52a0\u5165\u8be5\u793e\u56e2");
        }
        IndividualGroup exist = individualGroupMapper.checkExist(studentId, groupId);
        if (exist != null) {
            throw new BusinessException("APPLY_EXIST", "\u5df2\u7533\u8bf7\u6216\u5df2\u52a0\u5165\u8be5\u793e\u56e2");
        }
        IndividualGroup ig = new IndividualGroup();
        ig.setStudentId(studentId);
        ig.setGroupId(groupId);
        ig.setStatus(0);
        ig.setPosition("\u6210\u5458");
        try {
            individualGroupMapper.insert(ig);
        } catch (RuntimeException e) {
            RedisUtils.del(guardKey);
            throw e;
        }
    }

    @Override
    public void applyJoinGroupAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        applyJoinGroup(groupId, studentId);
        sendEvent("join_group_apply", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public List<IndividualGroup> getStudentsByGroup(int groupId, String searchInfo) {
        return individualGroupMapper.getStudentsByGroup(groupId, searchInfo);
    }

    @Override
    public List<IndividualGroup> getStudentsByGroupCached(int groupId, String searchInfo) {
        requireGroupId(groupId);
        String key = studentsKey(groupId);
        List<IndividualGroup> list = (List<IndividualGroup>) RedisUtils.get(key);
        if (list == null) {
            list = getStudentsByGroup(groupId, searchInfo);
            RedisUtils.set(key, list, 120);
        }
        return list;
    }

    @Override
    public List<IndividualGroup> getGroupApplyList(int groupId) {
        return individualGroupMapper.getGroupApplyList(groupId);
    }

    @Override
    public List<IndividualGroup> getGroupApplyListCached(int groupId) {
        requireGroupId(groupId);
        String key = applyKey(groupId);
        List<IndividualGroup> list = (List<IndividualGroup>) RedisUtils.get(key);
        if (list == null) {
            list = getGroupApplyList(groupId);
            RedisUtils.set(key, list, 60);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptJoin(int groupId, String studentId) {
        individualGroupMapper.updateStatus(groupId, studentId, 1);
    }

    @Override
    public void acceptJoinAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        acceptJoin(groupId, studentId);
        sendEvent("accept_group_apply", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectJoin(int groupId, String studentId) {
        individualGroupMapper.updateStatus(groupId, studentId, -1);
    }

    @Override
    public void rejectJoinAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        rejectJoin(groupId, studentId);
        sendEvent("reject_group_apply", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGroupStudent(int groupId, String studentId, String position) {
        IndividualGroup ig = new IndividualGroup();
        ig.setStudentId(studentId);
        ig.setGroupId(groupId);
        ig.setPosition(position);
        ig.setStatus(1);
        individualGroupMapper.insert(ig);
    }

    @Override
    public void addGroupStudentAndNotify(int groupId, String studentId, String position) {
        requireGroupStudent(groupId, studentId);
        addGroupStudent(groupId, studentId, position);
        sendEvent("add_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyGroupStudent(int groupId, String studentId, String position) {
        individualGroupMapper.updatePosition(groupId, studentId, position);
    }

    @Override
    public void modifyGroupStudentAndNotify(int groupId, String studentId, String position) {
        requireGroupStudent(groupId, studentId);
        modifyGroupStudent(groupId, studentId, position);
        sendEvent("modify_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroupStudent(int groupId, String studentId) {
        individualGroupMapper.delete(groupId, studentId);
        RedisUtils.del(groupApplyGuardKey(groupId, studentId));
    }

    @Override
    public void deleteGroupStudentAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        deleteGroupStudent(groupId, studentId);
        sendEvent("delete_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public List<Group> getAllManagedGroups(String managerId) {
        return individualGroupMapper.getAllManagedGroups(managerId);
    }

    @Override
    public List<Group> getAllManagedGroupsCached(String managerId) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        String key = managerKey(managerId);
        List<Group> list = (List<Group>) RedisUtils.get(key);
        if (list == null) {
            list = getAllManagedGroups(managerId);
            RedisUtils.set(key, list, 300);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferStatus(int groupId, String managerId, String userId) {
        individualGroupMapper.updateStatus(groupId, managerId, 0);
        individualGroupMapper.updateStatus(groupId, userId, 2);
    }

    @Override
    public void transferStatusAndNotify(int groupId, String managerId, String userId) {
        requireGroupId(groupId);
        requireText(managerId, "\u539f\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(userId, "\u65b0\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        transferStatus(groupId, managerId, userId);

        Map<String, Object> extra = new HashMap<>();
        extra.put("newManagerId", userId);
        sendEvent("transfer_manager", groupId, managerId, extra);
        RedisUtils.del(managerKey(managerId));
        RedisUtils.del(managerKey(userId));
        RedisUtils.del(studentsKey(groupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(int groupId, String studentId, int status) {
        individualGroupMapper.updateStatus(groupId, studentId, status);
    }

    @Override
    public void updatePermissionAndNotify(int groupId, String studentId, int status) {
        requireGroupStudent(groupId, studentId);
        updatePermission(groupId, studentId, status);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", status);
        sendEvent("update_permission", groupId, studentId, extra);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public List<IndividualGroup> getAllStudents(String searchInfo) {
        return individualGroupMapper.getAllStudents(searchInfo);
    }

    @Override
    public List<GroupNum> getGroupMembers() {
        return individualGroupMapper.getGroupMembers();
    }

    @Override
    public List<GroupNum> getGroupMembersCached() {
        String key = "individualGroup:top5:members";
        List<GroupNum> list = (List<GroupNum>) RedisUtils.get(key);
        if (list == null) {
            list = getGroupMembers();
            RedisUtils.set(key, list, 600);
        }
        return list;
    }

    private void requireGroupStudent(int groupId, String studentId) {
        requireGroupId(groupId);
        requireStudentId(studentId);
    }

    private void requireGroupId(int groupId) {
        if (groupId <= 0) {
            throw new BusinessException("PARAM_ERROR", "\u793e\u56e2ID\u4e0d\u5408\u6cd5");
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

    private void sendEvent(String type, int groupId, String studentId, Map<String, Object> extra) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("groupId", groupId);
        msg.put("studentId", studentId);
        if (extra != null) {
            msg.putAll(extra);
        }
        kafkaProducer.send(MqTopics.INDIVIDUAL_GROUP, JSONUtil.toJsonStr(msg));
    }

    private void evictGroupStudentCache(int groupId, String studentId) {
        RedisUtils.del(applyKey(groupId));
        RedisUtils.del(studentsKey(groupId));
        RedisUtils.del(studentKey(studentId));
        RedisUtils.del("individualGroup:top5:members");
    }

    private String groupApplyGuardKey(int groupId, String studentId) {
        return "individualGroup:apply:guard:" + groupId + ":" + studentId;
    }

    private String studentKey(String studentId) {
        return "individualGroup:student:" + studentId;
    }

    private String studentsKey(int groupId) {
        return "individualGroup:students:" + groupId;
    }

    private String applyKey(int groupId) {
        return "individualGroup:apply:" + groupId;
    }

    private String managerKey(String managerId) {
        return "individualGroup:manager:" + managerId;
    }
}
