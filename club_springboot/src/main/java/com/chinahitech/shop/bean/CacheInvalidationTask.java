package com.chinahitech.shop.bean;

import lombok.Data;

/** 与业务事务一起保存的缓存删除任务；只有删除成功后才移除记录。 */
@Data
public class CacheInvalidationTask {
    private String id;
    private String patterns;
}
