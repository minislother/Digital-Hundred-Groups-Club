package com.chinahitech.shop.utils;

import java.util.List;

public class PageResult<T> {

    private List<T> records;

    private long total;

    private int pageNum;

    private int pageSize;

    private int pages;

    private boolean hasPrevious;

    private boolean hasNext;

    /**
     * 读取并返回业务数据或页面状态。
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public long getTotal() {
        return total;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public int getPages() {
        return pages;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setPages(int pages) {
        this.pages = pages;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
