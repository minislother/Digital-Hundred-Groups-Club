package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.CacheInvalidationTask;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 持久化缓存失效任务，避免进程退出或 Kafka 不可用时丢失补偿。 */
public interface CacheInvalidationMapper {
    void insert(CacheInvalidationTask task);
    CacheInvalidationTask find(@Param("id") String id);
    List<CacheInvalidationTask> findDue();
    void complete(@Param("id") String id);
    void defer(@Param("id") String id);
}
