package org.stilllauncher.util

import org.stilllauncher.data.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object Formats {

    fun time(settings: Settings, dateTime: LocalDateTime = LocalDateTime.now()): String {
        val formatter = when (settings.timeFormat) {
            "24" -> DateTimeFormatter.ofPattern("HH:mm")
            "12" -> DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            else -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }
        return dateTime.format(formatter)
    }

    fun time(settings: Settings, time: LocalTime): String {
        val formatter = when (settings.timeFormat) {
            "24" -> DateTimeFormatter.ofPattern("HH:mm")
            "12" -> DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            else -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }
        return time.format(formatter)
    }

    fun date(settings: Settings, date: LocalDate = LocalDate.now()): String {
        val formatter = when (settings.dateFormat) {
            "iso" -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
            "dmy" -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
            "mdy" -> DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        }
        return date.format(formatter)
    }

    fun duration(millis: Long): String {
        if (millis <= 0L) return "0 m"
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0L -> "$minutes m"
            minutes == 0L -> "$hours h"
            else -> "$hours h $minutes m"
        }
    }

    fun minutesLabel(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    fun clock(minutesOfDay: Int): String =
        "%02d:%02d".format(Locale.US, minutesOfDay / 60, minutesOfDay % 60)

    fun relativeUntil(untilMillis: Long, now: Long = System.currentTimeMillis()): String {
        val remaining = abs(untilMillis - now)
        val minutes = remaining / 60_000L
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days >= 1 -> "${days}d ${hours % 24}h left"
            hours >= 1 -> "${hours}h ${minutes % 60}m left"
            minutes >= 1 -> "${minutes}m left"
            else -> "less than a minute left"
        }
    }

    fun daysLabel(days: Set<Int>): String = when {
        days.size == 7 -> "Every day"
        days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
        days == setOf(6, 7) -> "Weekends"
        days.isEmpty() -> "No days"
        else -> days.sorted().joinToString(", ") { dayName(it).take(3) }
    }

    fun dayName(day: Int): String = when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }
}
