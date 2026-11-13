package org.stilllauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsTest {

    @Test
    fun newestEntryGoesFirst() {
        val result = addRecentApp(
            current = listOf(RecentApp("first", 1L)),
            packageName = "second",
            now = 2L
        )
        assertEquals(listOf("second", "first"), result.map { it.packageName })
        assertEquals(2L, result.first().lastUsed)
    }

    @Test
    fun existingEntryMovesToFront() {
        val result = addRecentApp(
            current = listOf(
                RecentApp("a", 1L),
                RecentApp("b", 2L),
                RecentApp("c", 3L)
            ),
            packageName = "b",
            now = 9L
        )
        assertEquals(listOf("b", "a", "c"), result.map { it.packageName })
        assertEquals(9L, result.first().lastUsed)
    }

    @Test
    fun duplicatesAreRemoved() {
        val result = addRecentApp(
            current = listOf(
                RecentApp("a", 1L),
                RecentApp("b", 2L),
                RecentApp("a", 3L)
            ),
            packageName = "a",
            now = 4L
        )
        assertEquals(listOf("a", "b"), result.map { it.packageName })
    }

    @Test
    fun listIsCapped() {
        var list = emptyList<RecentApp>()
        repeat(12) { index ->
            list = addRecentApp(list, "app$index", index.toLong())
        }
        assertEquals(8, list.size)
        assertEquals("app11", list.first().packageName)
        assertEquals("app4", list.last().packageName)
    }
}
