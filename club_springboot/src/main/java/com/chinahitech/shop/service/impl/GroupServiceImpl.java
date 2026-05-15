package com.chinahitech.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.Group;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.GroupMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import com.chinahitech.shop.service.GroupService;
import com.chinahitech.shop.utils.CacheData;
import com.chinahitech.shop.utils.RedisUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 社团业务实现，处理社团列表/详情缓存、社团资料维护和社团审核通知。
 */
@Service
public class GroupServiceImpl implements GroupService {

    private static final String GROUP_CACHE_PATTERN = "group:*";
    private static final long GROUP_DETAIL_LOGICAL_TTL_MINUTES = 10L;
    private static final long GROUP_DETAIL_NULL_TTL_SECONDS = 2 * 60L;
    private static final long GROUP_DETAIL_REBUILD_LOCK_SECONDS = 10L;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(2);

    @Resource
    private GroupMapper groupMapper;

    @Resource
    private KafkaProducer kafkaProducer;

    // Student-facing queries.
    @Override
    public List<Group> query(String searchInfo) {
        return groupMapper.query(searchInfo);
    }

    @Override
    public List<Group> queryCached(String searchInfo) {
        String key = "group:all:" + (searchInfo == null ? "" : searchInfo);
        List<Group> groups = (List<Group>) RedisUtils.get(key);
        if (groups == null) {
            groups = query(searchInfo);
            RedisUtils.set(key, groups, 300);
        }
        return groups;
    }

    @Override
    public Group getByName(String groupName) {
        return groupMapper.getByName(groupName);
    }

    @Override
    public Group getCachedGroupDetail(String groupName) {
        String key = groupDetailKey(groupName);
        Object cached = RedisUtils.get(key);
        if (RedisUtils.NULL_VALUE.equals(cached)) {
            return null;
        }
        if (cached instanceof CacheData) {
            CacheData<?> cacheData = (CacheData<?>) cached;
            Group group = (Group) cacheData.getData();
            if (cacheData.getExpireTime() != null && cacheData.getExpireTime().isAfter(LocalDateTime.now())) {
                return group;
            }
            tryRebuildGroupDetailCache(key, groupName);
            return group;
        }
        if (cached instanceof Group) {
            return (Group) cached;
        }

        Group group = groupMapper.getByName(groupName);
        saveGroupDetailCache(key, group);
        return group;
    }

    @Override
    public List<Group> queryTop() {
        return groupMapper.queryTop();
    }

    @Override
    public List<Group> queryTopCached() {
        String key = "group:top";
        List<Group> groups = (List<Group>) RedisUtils.get(key);
        if (groups == null) {
            groups = queryTop();
            RedisUtils.set(key, groups, 120);
        }
        return groups;
    }

    @Override
    public void insert(Group group) {
        group.setIsAccepted(null);
        groupMapper.insert(group);
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "addGroup");
        msg.put("data", group);
        kafkaProducer.send(MqTopics.GROUP, JSONUtil.toJsonStr(msg));
        evictGroupCache();
    }

    @Override
    public void addHot(String groupName) {
        groupMapper.addHot(groupName);
        evictGroupCache();
    }

    // Manager-facing maintenance.
    @Override
    public void updateDescription(String groupname, String description, String attachment, String image) {
        groupMapper.updateDescription(groupname, description, attachment, image);
        evictGroupCache();
    }

    @Override
    public void updateAttachment(String name, String attachment) {
        groupMapper.updateAttachment(name, attachment);
        evictGroupCache();
    }

    @Override
    public void updateImage(String name, String image) {
        groupMapper.updateImage(name, image);
        evictGroupCache();
    }

    @Override
    public Group getGroupById(Integer id) {
        Group group = groupMapper.getGroupById(id);
        if (group == null) {
            throw new BusinessException("GROUP_NOT_EXIST", "\u793e\u56e2\u4e0d\u5b58\u5728");
        }
        return group;
    }

    @Override
    public String getAttachment(Integer id) {
        return getGroupById(id).getAttachment();
    }

    // Super-admin approval.
    @Override
    public List<Group> getAllApp(String searchinfo) {
        return groupMapper.getAllApp(searchinfo);
    }

    @Override
    public Group getAppByName(String groupname) {
        return groupMapper.getAppByName(groupname);
    }

    @Override
    public void confirmApplication(Integer groupId) {
        groupMapper.confirmApplication(groupId);
        evictGroupCache();
    }

    @Override
    public void denyApplication(Integer groupId) {
        groupMapper.denyApplication(groupId);
        evictGroupCache();
    }

    private String groupDetailKey(String groupName) {
        return "group:detail:" + groupName;
    }

    private String groupDetailLockKey(String groupName) {
        return "lock:group:detail:" + groupName;
    }

    private void tryRebuildGroupDetailCache(String key, String groupName) {
        String lockKey = groupDetailLockKey(groupName);
        if (!RedisUtils.setIfAbsent(lockKey, "1", GROUP_DETAIL_REBUILD_LOCK_SECONDS)) {
            return;
        }
        CACHE_REBUILD_EXECUTOR.execute(() -> {
            try {
                Group fresh = groupMapper.getByName(groupName);
                saveGroupDetailCache(key, fresh);
            } finally {
                RedisUtils.del(lockKey);
            }
        });
    }

    private void saveGroupDetailCache(String key, Group group) {
        if (group == null) {
            RedisUtils.set(key, RedisUtils.NULL_VALUE, GROUP_DETAIL_NULL_TTL_SECONDS);
            return;
        }
        RedisUtils.set(key, new CacheData<>(group, LocalDateTime.now().plusMinutes(GROUP_DETAIL_LOGICAL_TTL_MINUTES)));
    }

    private void evictGroupCache() {
        if (!RedisUtils.del(GROUP_CACHE_PATTERN)) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "group_cache_evict");
            msg.put("pattern", GROUP_CACHE_PATTERN);
            kafkaProducer.send(MqTopics.GROUP_CACHE, JSONUtil.toJsonStr(msg));
        }
    }
}
