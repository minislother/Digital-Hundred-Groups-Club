package com.chinahitech.shop.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void okCreatesSuccessfulResultWithDefaultCode() {
        Result result = Result.ok();

        assertTrue(result.getSuccess());
        assertEquals(ResultCode.SUCCESS, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void errorCreatesFailedResultWithDefaultCode() {
        Result result = Result.error();

        assertFalse(result.getSuccess());
        assertEquals(ResultCode.ERROR, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void chainMethodsMutateAndReturnSameInstance() {
        Result result = Result.ok();

        Result chained = result
                .success(false)
                .code(400)
                .message("failed")
                .data("id", 12);

        assertSame(result, chained);
        assertFalse(result.getSuccess());
        assertEquals(400, result.getCode());
        assertEquals("failed", result.getMessage());
        assertEquals(12, result.getData().get("id"));
    }

    @Test
    void dataMapReplacesExistingDataMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "club");

        Result result = Result.ok().data("old", "value").data(data);

        assertSame(data, result.getData());
        assertEquals("club", result.getData().get("name"));
        assertFalse(result.getData().containsKey("old"));
    }
}
