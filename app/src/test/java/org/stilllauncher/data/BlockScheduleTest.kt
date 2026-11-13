package org.stilllauncher.data

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockScheduleTest {

    private val overnight = BlockSchedule(
        name = "Night",
        days = setOf(1, 2, 3, 4, 5, 6, 7),
        startMinute = 22 * 60,
        endMinute = 7 * 60,
        packages = listOf("com.example.app")
    )

    private val workHours = BlockSchedule(
        name = "Work",
        days = setOf(1, 2, 3, 4, 5),
        startMinute = 9 * 60,
        endMinute = 17 * 60,
        packages = listOf("com.example.app")
    )

    @Test
    fun overnightScheduleIsActiveLateEvening() {
        assertTrue(overnight.isActiveAt(LocalDateTime.of(2026, 11, 9, 23, 0)))
    }

    @Test
    fun overnightScheduleIsActiveAfterMidnight() {
        assertTrue(overnight.isActiveAt(LocalDateTime.of(2026, 11, 10, 0, 30)))
    }

    @Test
    fun overnightScheduleEndsAtSeven() {
        assertFalse(overnight.isActiveAt(LocalDateTime.of(2026, 11, 10, 7, 0)))
    }

    @Test
    fun overnightScheduleIsInactiveDuringDay() {
        assertFalse(overnight.isActiveAt(LocalDateTime.of(2026, 11, 10, 12, 0)))
    }

    @Test
    fun weekdayScheduleIsActiveOnMondayMorning() {
        assertTrue(workHours.isActiveAt(LocalDateTime.of(2026, 11, 9, 10, 0)))
    }

    @Test
    fun weekdayScheduleIsInactiveOnSaturday() {
        assertFalse(workHours.isActiveAt(LocalDateTime.of(2026, 11, 14, 10, 0)))
    }

    @Test
    fun weekdayScheduleIsInactiveAfterEnd() {
        assertFalse(workHours.isActiveAt(LocalDateTime.of(2026, 11, 9, 17, 0)))
    }

    @Test
    fun overnightEndIsTomorrowInTheEvening() {
        val now = LocalDateTime.of(2026, 11, 9, 23, 0)
        assertEquals(LocalDateTime.of(2026, 11, 10, 7, 0), overnight.nextEnd(now))
    }

    @Test
    fun overnightEndIsTodayAfterMidnight() {
        val now = LocalDateTime.of(2026, 11, 10, 0, 30)
        assertEquals(LocalDateTime.of(2026, 11, 10, 7, 0), overnight.nextEnd(now))
    }

    @Test
    fun disabledScheduleNeverBlocks() {
        assertFalse(overnight.copy(enabled = false).isActiveAt(LocalDateTime.of(2026, 11, 9, 23, 0)))
    }
}
