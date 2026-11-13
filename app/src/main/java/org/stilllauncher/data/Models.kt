package org.stilllauncher.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Serializable
data class BlockedApp(
    val packageName: String,
    val blockedUntil: Long
)

@Serializable
data class BlockSchedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Focus time",
    val enabled: Boolean = true,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    val packages: List<String> = emptyList()
) {
    fun isActiveAt(now: LocalDateTime): Boolean {
        if (!enabled || packages.isEmpty()) return false
        val day = now.dayOfWeek.value
        val minute = now.hour * 60 + now.minute
        return if (startMinute <= endMinute) {
            day in days && minute >= startMinute && minute < endMinute
        } else {
            (day in days && minute >= startMinute) ||
                (previousDay(day) in days && minute < endMinute)
        }
    }

    fun nextEnd(now: LocalDateTime): LocalDateTime {
        var end = now.toLocalDate().atTime(endMinute / 60, endMinute % 60)
        if (startMinute > endMinute) {
            val minute = now.hour * 60 + now.minute
            if (minute >= startMinute) end = end.plusDays(1)
        } else if (!end.isAfter(now)) {
            end = end.plusDays(1)
        }
        return end
    }

    fun nextEndMillis(now: LocalDateTime = LocalDateTime.now()): Long =
        nextEnd(now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun previousDay(day: Int): Int = if (day == 1) 7 else day - 1
}

@Serializable
enum class ReminderBehavior {
    ONLY_REMIND,
    EXIT_APP,
    COOLDOWN
}

@Serializable
data class AppLimit(
    val packageName: String,
    val minutes: Int,
    val behavior: ReminderBehavior = ReminderBehavior.ONLY_REMIND,
    val lastRemindedAt: Long = 0L,
    val cooldownUntil: Long = 0L
)

@Serializable
data class AppFolder(
    val name: String,
    val packages: List<String> = emptyList()
)

data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystem: Boolean
)

data class Settings(
    val introDone: Boolean = false,
    val themeId: String = "system",
    val customColor: Long = 0xFFF4F1EA,
    val customDark: Boolean = false,
    val fontId: String = "default",
    val fontSizeSp: Int = 16,
    val timeFormat: String = "system",
    val dateFormat: String = "system",
    val swipeUpSearch: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    val autoKeyboard: Boolean = false,
    val doubleTapLock: Boolean = false,
    val monitorEnabled: Boolean = true,
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val renamed: Map<String, String> = emptyMap(),
    val blocked: List<BlockedApp> = emptyList(),
    val schedules: List<BlockSchedule> = emptyList(),
    val limits: List<AppLimit> = emptyList(),
    val reminderMaster: Boolean = true,
    val shortVideoBlocking: Boolean = false,
    val monochromeEnabled: Boolean = false,
    val monochromePackages: Set<String> = emptySet(),
    val notificationManagerActive: Boolean = false,
    val notificationAllowed: Set<String> = emptySet()
)

fun BlockSchedule.isActiveNow(): Boolean = isActiveAt(LocalDateTime.now())

fun LocalDate.epochMillisAtStartOfDay(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
