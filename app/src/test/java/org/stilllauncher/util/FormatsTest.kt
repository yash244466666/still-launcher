package org.stilllauncher.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {

    @Test
    fun zeroUsageReadsAsZeroMinutes() {
        assertEquals("0 m", Formats.duration(0L))
    }

    @Test
    fun subMinuteUsageIsNotRoundedDownToZero() {
        assertEquals("<1 m", Formats.duration(30_000L))
    }

    @Test
    fun minutesAndHoursAreCombined() {
        assertEquals("1 h 5 m", Formats.duration(65L * 60_000L))
        assertEquals("2 h", Formats.duration(2L * 60L * 60_000L))
    }
}
