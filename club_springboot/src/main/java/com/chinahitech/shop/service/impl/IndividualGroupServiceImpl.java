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
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社团成员关系实现，处理入团申请、管理员审核/维护成员、权限转移、缓存失效和 MQ 通知。
 * 管理端写操作会先校验当前管理员是否有权管理目标社团。
 */
@Service
public class IndividualGroupServiceImpl implements IndividualGroupService {

    private static final long APPLY_GUARD_TTL_SECONDS = 24 * 60 * 60L;

    @Autowired
    private IndividualGroupMapper individualGroupMapper;

    @Autowired
    private com.chinahitech.shop.mapper.GroupMapper groupPageMapper;

    /** 分页请求直接查询当前页，避免全量成员进入应用内存。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<IndividualGroup> getMembershipPage(
            int groupId, int status, String searchInfo, Integer pageNum, Integer pageSize) {
        var records = individualGroupMapper.getMembershipPage(groupId, status, searchInfo,
                PageUtils.offset(pageNum, pageSize), PageUtils.limit(pageSize));
        long total = individualGroupMapper.countMembership(groupId, status, searchInfo);
        return PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    @Autowired
    private GroupService groupService;

    @Autowired
    private KafkaProducer kafkaProducer;

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getGroupByStuId(String studentId) {
        return individualGroupMapper.getGroupByStuId(studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Map<String, Object> getStudentGroupsCached(String studentId) {
        requireStudentId(studentId);
        String key = studentKey(studentId);
        Map<String, Object> map = (Map<String, Object>) RedisUtils.getBusinessCache(key);
        if (map == null) {
            List<IndividualGroup> individualGroupList = getGroupByStuId(studentId);
            List<Group> groupList = new ArrayList<>();
            for (IndividualGroup individualGroup : individualGroupList) {
                groupList.add(groupService.getGroupById(individualGroup.getGroupId()));
            }

            map = new HashMap<>();
            map.put("stuItems", individualGroupList);
            map.put("groupItems", groupList);
            RedisUtils.setBusinessCache(key, map, 300);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoinGroup(int groupId, String studentId) {
        // 先查数据库再写 Redis guard，避免已有申请路径留下防重复缓存。
        IndividualGroup exist = individualGroupMapper.checkExist(studentId, groupId);
        if (exist != null) {
            throw new BusinessException("APPLY_EXIST", "\u5df2\u7533\u8bf7\u6216\u5df2\u52a0\u5165\u8be5\u793e\u56e2");
        }
        String guardKey = groupApplyGuardKey(groupId, studentId);
        if (!RedisUtils.setIfAbsent(guardKey, "1", APPLY_GUARD_TTL_SECONDS)) {
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

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getStudentsByGroup(int groupId, String searchInfo) {
        return individualGroupMapper.getStudentsByGroup(groupId, searchInfo);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getStudentsByGroupCached(int groupId, String searchInfo) {
        requireGroupId(groupId);
        String key = studentsKey(groupId);
        List<IndividualGroup> list = (List<IndividualGroup>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getStudentsByGroup(groupId, searchInfo);
            RedisUtils.setBusinessCache(key, list, 120);
        }
        return list;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getGroupApplyList(int groupId) {
        return individualGroupMapper.getGroupApplyList(groupId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getGroupApplyListCached(int groupId) {
        requireGroupId(groupId);
        String key = applyKey(groupId);
        List<IndividualGroup> list = (List<IndividualGroup>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getGroupApplyList(groupId);
            RedisUtils.setBusinessCache(key, list, 60);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptJoin(int groupId, String studentId) {
        ensureUpdated(individualGroupMapper.updateStatus(groupId, studentId, 1));
    }

    @Override
    public void acceptJoinAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        acceptJoin(groupId, studentId);
        sendEvent("accept_group_apply", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public void acceptJoinAndNotify(int groupId, String studentId, String managerId) {
        assertManagedGroup(groupId, managerId);
        acceptJoinAndNotify(groupId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectJoin(int groupId, String studentId) {
        ensureUpdated(individualGroupMapper.updateStatus(groupId, studentId, -1));
    }

    @Override
    public void rejectJoinAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        rejectJoin(groupId, studentId);
        sendEvent("reject_group_apply", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public void rejectJoinAndNotify(int groupId, String studentId, String managerId) {
        assertManagedGroup(groupId, managerId);
        rejectJoinAndNotify(groupId, studentId);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
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

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addGroupStudentAndNotify(int groupId, String studentId, String position) {
        requireGroupStudent(groupId, studentId);
        /**
         * 新增业务数据或追加界面状态。
         */
        addGroupStudent(groupId, studentId, position);
        sendEvent("add_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addGroupStudentAndNotify(int groupId, String studentId, String position, String managerId) {
        assertManagedGroup(groupId, managerId);
        /**
         * 新增业务数据或追加界面状态。
         */
        addGroupStudentAndNotify(groupId, studentId, position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyGroupStudent(int groupId, String studentId, String position) {
        ensureUpdated(individualGroupMapper.updatePosition(groupId, studentId, position));
    }

    @Override
    public void modifyGroupStudentAndNotify(int groupId, String studentId, String position) {
        requireGroupStudent(groupId, studentId);
        modifyGroupStudent(groupId, studentId, position);
        sendEvent("modify_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    @Override
    public void modifyGroupStudentAndNotify(int groupId, String studentId, String position, String managerId) {
        assertManagedGroup(groupId, managerId);
        modifyGroupStudentAndNotify(groupId, studentId, position);
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroupStudent(int groupId, String studentId) {
        ensureUpdated(individualGroupMapper.delete(groupId, studentId));
        RedisUtils.del(groupApplyGuardKey(groupId, studentId));
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    public void deleteGroupStudentAndNotify(int groupId, String studentId) {
        requireGroupStudent(groupId, studentId);
        /**
         * 删除指定业务数据。
         */
        deleteGroupStudent(groupId, studentId);
        sendEvent("delete_group_student", groupId, studentId, null);
        evictGroupStudentCache(groupId, studentId);
    }

    /**
     * 删除指定业务数据。
     */
    @Override
    public void deleteGroupStudentAndNotify(int groupId, String studentId, String managerId) {
        assertManagedGroup(groupId, managerId);
        /**
         * 删除指定业务数据。
         */
        deleteGroupStudentAndNotify(groupId, studentId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Group> getAllManagedGroups(String managerId) {
        return individualGroupMapper.getAllManagedGroups(managerId);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<Group> getAllManagedGroupsCached(String managerId) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        String key = managerKey(managerId);
        List<Group> list = (List<Group>) RedisUtils.getBusinessCache(key);
        if (list == null) {
            list = getAllManagedGroups(managerId);
            RedisUtils.setBusinessCache(key, list, 300);
        }
        return list;
    }

    @Override
    public PageResult<Group> getAllManagedGroupsPageCached(String managerId,
                                                            Integer pageNum,
                                                            Integer pageSize) {
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        int offset = PageUtils.offset(pageNum, pageSize);
        int limit = PageUtils.limit(pageSize);
        // 使用 group 前缀，让社团新增、修改和审核时的统一失效逻辑能清除此分页缓存。
        String key = "group:managed:" + managerId + ':' + offset + ':' + limit;
        PageResult<Group> page = (PageResult<Group>) RedisUtils.getBusinessCache(key);
        if (page == null) {
            List<Group> records = individualGroupMapper.getAllManagedGroupsPage(managerId, offset, limit);
            long total = individualGroupMapper.countAllManagedGroups(managerId);
            page = PageUtils.pageResult(records, total, pageNum, pageSize);
            RedisUtils.setBusinessCache(key, page, 300);
        }
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferStatus(int groupId, String managerId, String userId) {
        ensureUpdated(individualGroupMapper.updateStatus(groupId, managerId, 0));
        ensureUpdated(individualGroupMapper.updateStatus(groupId, userId, 2));
    }

    @Override
    public void transferStatusAndNotify(int groupId, String managerId, String userId) {
        requireGroupId(groupId);
        requireText(managerId, "\u539f\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(userId, "\u65b0\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        assertManagedGroup(groupId, managerId);
        transferStatus(groupId, managerId, userId);

        Map<String, Object> extra = new HashMap<>();
        extra.put("newManagerId", userId);
        sendEvent("transfer_manager", groupId, managerId, extra);
        RedisUtils.del(managerKey(managerId));
        RedisUtils.del(managerKey(userId));
        RedisUtils.del(studentsKey(groupId));
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(int groupId, String studentId, int status) {
        ensureUpdated(individualGroupMapper.updateStatus(groupId, studentId, status));
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updatePermissionAndNotify(int groupId, String studentId, int status) {
        requireGroupStudent(groupId, studentId);
        /**
         * 更新表单字段或业务数据。
         */
        updatePermission(groupId, studentId, status);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", status);
        sendEvent("update_permission", groupId, studentId, extra);
        evictGroupStudentCache(groupId, studentId);
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updatePermissionAndNotify(int groupId, String studentId, int status, String managerId) {
        assertManagedGroup(groupId, managerId);
        /**
         * 更新表单字段或业务数据。
         */
        updatePermissionAndNotify(groupId, studentId, status);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<IndividualGroup> getAllStudents(String searchInfo) {
        return individualGroupMapper.getAllStudents(searchInfo);
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<GroupNum> getGroupMembers() {
        return individualGroupMapper.getGroupMembers();
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public List<GroupNum> getGroupMembersCached() {
        String key = "individualGroup:top5:members";
        List<GroupNum> list = (List<GroupNum>) RedisUtils.getBusinessCache(key);
        if (list == null || hasInvalidGroupMemberStats(list)) {
            list = getGroupMembers();
            RedisUtils.setBusinessCache(key, list, 600);
        }
        return list;
    }

    private boolean hasInvalidGroupMemberStats(List<GroupNum> list) {
        for (GroupNum groupNum : list) {
            if (groupNum == null || groupNum.getGroupName() == null || groupNum.getGroupName().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void requireGroupStudent(int groupId, String studentId) {
        requireGroupId(groupId);
        requireStudentId(studentId);
    }

    private void assertManagedGroup(int groupId, String managerId) {
        requireGroupId(groupId);
        requireText(managerId, "\u7ba1\u7406\u5458ID\u4e0d\u80fd\u4e3a\u7a7a");
        // create_user 和 leader 都视为该社团的合法管理员。
        if (individualGroupMapper.countManagedGroup(groupId, managerId) <= 0) {
            throw new BusinessException("FORBIDDEN", "\u65e0\u6743\u64cd\u4f5c\u8be5\u793e\u56e2");
        }
    }

    /**
     * 校验社团 ID 是否有效，无效时抛出业务异常。
     */
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

    private void ensureUpdated(int rows) {
        // update/delete 影响 0 行表示目标申请或成员关系不存在，不能返回业务成功。
        if (rows <= 0) {
            throw new BusinessException("NOT_FOUND", "\u672a\u627e\u5230\u53ef\u66f4\u65b0\u7684\u793e\u56e2\u6210\u5458\u8bb0\u5f55");
        }
    }

    /**
     * 发送业务事件消息，用于异步同步或缓存刷新。
     */
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

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<IndividualGroup> getAllStudentsPage(String searchInfo, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = individualGroupMapper.getAllStudentsPage(searchInfo, offset, limit);
        long total = individualGroupMapper.getAllStudentsCount(searchInfo);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 下一页多取一条判断 hasNext，不执行总数统计。 */
    @Override
    public com.chinahitech.shop.utils.Result studentSlice(String studentId, Long beforeId, Integer pageSize) {
        if (beforeId != null && beforeId <= 0) throw new com.chinahitech.shop.exception.BusinessException("PARAM_ERROR", "游标必须为正数");
        int size = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var rows = individualGroupMapper.studentSlice(studentId, beforeId, size + 1);
        boolean hasNext = rows.size() > size;
        var items = rows.subList(0, Math.min(size, rows.size()));
        var ids = items.stream().map(IndividualGroup::getGroupId).distinct().toList();
        var groups = ids.isEmpty() ? java.util.List.of() : groupPageMapper.findByIds(ids);
        return com.chinahitech.shop.utils.Result.ok().data("items", items).data("stuItems", items)
                .data("groupItems", groups).data("hasNext", hasNext)
                .data("nextCursor", hasNext ? items.getLast().getId() : null);
    }

    @Override
    public com.chinahitech.shop.utils.Result membershipTreePage(String searchInfo, Integer pageNum, Integer pageSize) {
        var page = getAllStudentsPage(searchInfo, pageNum, pageSize);
        var ids = page.getRecords().stream().map(IndividualGroup::getGroupId).distinct().toList();
        var groups = ids.isEmpty() ? java.util.List.of() : groupPageMapper.findByIds(ids);
        return PageUtils.ok("items", page).data("groups", groups);
    }

    @Override
    public com.chinahitech.shop.utils.Result studentGroupsPage(String studentId, Integer pageNum, Integer pageSize) {
        var records = individualGroupMapper.studentPage(studentId, PageUtils.offset(pageNum, pageSize), PageUtils.limit(pageSize));
        var ids = records.stream().map(IndividualGroup::getGroupId).distinct().toList();
        var groups = ids.isEmpty() ? java.util.List.of() : groupPageMapper.findByIds(ids);
        var page = PageUtils.pageResult(records, individualGroupMapper.countStudent(studentId), pageNum, pageSize);
        return PageUtils.ok("items", page).data("stuItems", records).data("groupItems", groups);
    }

    /** 高增长成员或申请列表按不可变关系 ID 连续翻页，保留管理员资源范围校验。 */
    @Override
    public com.chinahitech.shop.utils.Result memberSlice(int resourceId, int status, Long beforeId, Integer pageSize, String managerId) {
        assertManagedGroup(resourceId, managerId);
        if ((status != 0 && status != 1) || (beforeId != null && beforeId <= 0)) {
            throw new com.chinahitech.shop.exception.BusinessException("PARAM_ERROR", "成员游标或状态不合法");
        }
        int size = PageUtils.limit(pageSize);
        var rows = individualGroupMapper.memberSlice(resourceId, status, beforeId, size + 1);
        boolean hasNext = rows.size() > size;
        var items = rows.subList(0, Math.min(size, rows.size()));
        return com.chinahitech.shop.utils.Result.ok().data("items", items).data("hasNext", hasNext)
                .data("nextCursor", hasNext ? items.getLast().getId() : null);
    }
}
