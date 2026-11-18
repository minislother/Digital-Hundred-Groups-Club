package com.chinahitech.shop.utils;

import java.util.Collections;
import java.util.List;

public final class PageUtils {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    public static <T> Result ok(String itemKey, List<T> source, Integer pageNum, Integer pageSize) {
        if (!isPageRequest(pageNum, pageSize)) {
            return Result.ok().data(itemKey, safeList(source));
        }

        PageResult<T> page = page(source, pageNum, pageSize);
        return Result.ok()
                .data(itemKey, page.getRecords())
                .data("page", page);
    }

    /**
     * 将 Service 已经在数据库中分页的结果包装成统一响应，避免 Controller 再次切页。
     */
    public static <T> Result ok(String itemKey, PageResult<T> page) {
        return Result.ok()
                .data(itemKey, page.getRecords())
                .data("page", page);
    }

    /** 判断调用方是否明确请求分页；未传分页参数时继续兼容旧的全量接口行为。 */
    public static boolean isPageRequest(Integer pageNum, Integer pageSize) {
        return pageNum != null || pageSize != null;
    }

    /**
     * 根据数据库返回的当前页记录和总行数构造分页元数据。
     */
    public static <T> PageResult<T> pageResult(List<T> records, long total,
                                                Integer pageNum, Integer pageSize) {
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);

        PageResult<T> page = new PageResult<>();
        page.setRecords(safeList(records));
        page.setTotal(total);
        page.setPageNum(normalizedPageNum);
        page.setPageSize(normalizedPageSize);
        page.setPages(pages);
        page.setHasPrevious(normalizedPageNum > 1 && total > 0);
        page.setHasNext(normalizedPageNum < pages);
        return page;
    }

    /** 计算 SQL OFFSET，页码和每页数量会先按系统上限进行归一化。 */
    public static int offset(Integer pageNum, Integer pageSize) {
        long offset = (long) (normalizePageNum(pageNum) - 1) * normalizePageSize(pageSize);
        if (offset > 10000) {
            throw new com.chinahitech.shop.exception.BusinessException("PAGE_TOO_DEEP",
                    "页码超出查询范围，请缩小筛选范围或使用游标分页");
        }
        return (int) offset;
    }

    /** 返回限制在 1 到 100 之间的数据库分页大小。 */
    public static int limit(Integer pageSize) {
        return normalizePageSize(pageSize);
    }

    private static <T> PageResult<T> page(List<T> source, Integer pageNum, Integer pageSize) {
        List<T> safeSource = safeList(source);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int total = safeSource.size();
        int fromIndex = Math.min(offset(pageNum, pageSize), total);
        int toIndex = Math.min(fromIndex + normalizedPageSize, total);

        return pageResult(safeSource.subList(fromIndex, toIndex), total,
                normalizedPageNum, normalizedPageSize);
    }

    private static int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
