package com.minislother.baituan.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * StableListKeyTest 验证 Compose 列表 key 的生成规则。
 *
 * LazyColumn 的 key 不能重复；后端数据如果 id 缺失或名称重复，稳定 key 可以降低页面崩溃风险。
 */
class StableListKeyTest {

    @Test
    fun testStableListKey_WithBusinessFields_ReturnReadableKey() {
        // Given - 准备区块名、列表下标和业务字段
        val section = "activity-ranking"

        // When - 生成 Compose 列表 key
        val result = stableListKey(section, 0, 12, "篮球赛")

        // Then - key 应包含足够信息，方便区分列表项
        assertEquals("activity-ranking-0-12-篮球赛", result)
    }

    @Test
    fun testStableListKey_NullAndBlankParts_FilterAndNormalize() {
        // Given - 准备 null 和空字符串字段
        val section = "club"

        // When - 生成列表 key
        val result = stableListKey(section, 1, null, "")

        // Then - null 不参与拼接，空字符串会被替换成 blank
        assertEquals("club-1-blank", result)
    }

    @Test
    fun testStableListKey_SameBusinessFieldDifferentIndex_NotEqual() {
        // Given - 准备业务字段相同但下标不同的两条数据
        val first = stableListKey("user", 0, 1001)
        val second = stableListKey("user", 1, 1001)

        // Then - 下标参与 key，避免同一列表内重复
        assertNotEquals(first, second)
    }
}
