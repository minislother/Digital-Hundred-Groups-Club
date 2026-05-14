package com.chinahitech.shop.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageUtilsTest {

    @Test
    void okReturnsFullListWhenNoPageRequestIsProvided() {
        List<String> source = Arrays.asList("a", "b", "c");

        Result result = PageUtils.ok("items", source, null, null);

        assertEquals(ResultCode.SUCCESS, result.getCode());
        assertSame(source, result.getData().get("items"));
        assertFalse(result.getData().containsKey("page"));
    }

    @Test
    void okReturnsRequestedPageAndMetadata() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5);

        Result result = PageUtils.ok("items", source, 2, 2);

        assertEquals(Arrays.asList(3, 4), result.getData().get("items"));

        PageResult<Integer> page = getPage(result);
        assertEquals(Arrays.asList(3, 4), page.getRecords());
        assertEquals(5, page.getTotal());
        assertEquals(2, page.getPageNum());
        assertEquals(2, page.getPageSize());
        assertEquals(3, page.getPages());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void okNormalizesInvalidPageArguments() {
        List<Integer> source = Arrays.asList(1, 2, 3);

        Result result = PageUtils.ok("items", source, 0, 0);

        assertEquals(source, result.getData().get("items"));

        PageResult<Integer> page = getPage(result);
        assertEquals(1, page.getPageNum());
        assertEquals(10, page.getPageSize());
        assertEquals(1, page.getPages());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    void okHandlesNullSourceAsEmptyList() {
        Result result = PageUtils.ok("items", null, 1, 10);

        assertEquals(Collections.emptyList(), result.getData().get("items"));

        PageResult<Object> page = getPage(result);
        assertEquals(Collections.emptyList(), page.getRecords());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getPages());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    void okCapsOversizedPageSize() {
        List<Integer> source = Arrays.asList(1, 2, 3);

        Result result = PageUtils.ok("items", source, 1, 1000);

        PageResult<Integer> page = getPage(result);
        assertEquals(100, page.getPageSize());
        assertEquals(source, page.getRecords());
    }

    @SuppressWarnings("unchecked")
    private static <T> PageResult<T> getPage(Result result) {
        return (PageResult<T>) result.getData().get("page");
    }
}
