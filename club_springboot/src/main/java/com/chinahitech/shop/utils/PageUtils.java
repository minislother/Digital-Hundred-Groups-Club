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

    private static boolean isPageRequest(Integer pageNum, Integer pageSize) {
        return pageNum != null || pageSize != null;
    }

    private static <T> PageResult<T> page(List<T> source, Integer pageNum, Integer pageSize) {
        List<T> safeSource = safeList(source);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        int total = safeSource.size();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);
        int fromIndex = Math.min((normalizedPageNum - 1) * normalizedPageSize, total);
        int toIndex = Math.min(fromIndex + normalizedPageSize, total);

        PageResult<T> page = new PageResult<>();
        page.setRecords(safeSource.subList(fromIndex, toIndex));
        page.setTotal(total);
        page.setPageNum(normalizedPageNum);
        page.setPageSize(normalizedPageSize);
        page.setPages(pages);
        page.setHasPrevious(normalizedPageNum > 1 && total > 0);
        page.setHasNext(normalizedPageNum < pages);
        return page;
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
