package org.stilllauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object UsageRepository {

    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun startOfToday(): Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    fun startOfWeek(): Long = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun startOfMonth(): Long = LocalDate.now().withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun foregroundPackage(context: Context): String? {
        if (!hasAccess(context)) return null
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = runCatching { manager.queryEvents(now - 30_000, now) }.getOrNull()
            ?: return null
        val event = UsageEvents.Event()
        var lastPackage: String? = null
        var lastTimestamp = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if ((type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == UsageEvents.Event.MOVE_TO_FOREGROUND) &&
                event.timeStamp >= lastTimestamp
            ) {
                lastPackage = event.packageName
                lastTimestamp = event.timeStamp
            }
        }
        return lastPackage
    }

    fun usageSince(context: Context, packageName: String, since: Long): Long {
        return usageBetween(context, since, System.currentTimeMillis())[packageName] ?: 0L
    }

    /**
     * Reconstructs foreground intervals from usage events. This mirrors the
     * approach used by the original app: RESUMED opens an interval and
     * PAUSED/STOPPED closes it. Intervals still open at [end] are counted up
     * to [end].
     */
    fun usageBetween(context: Context, start: Long, end: Long): Map<String, Long> {
        if (!hasAccess(context)) return emptyMap()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val events = runCatching { manager.queryEvents(start, end) }.getOrNull()
            ?: return emptyMap()
        val open = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> open[pkg] = event.timeStamp

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val started = open.remove(pkg) ?: continue
                    val delta = (event.timeStamp - started).coerceAtLeast(0L)
                    totals[pkg] = (totals[pkg] ?: 0L) + delta
                }
            }
        }
        open.forEach { (pkg, started) ->
            val delta = (end - started).coerceAtLeast(0L)
            totals[pkg] = (totals[pkg] ?: 0L) + delta
        }
        return totals
    }

    fun sortedUsage(context: Context, start: Long): List<Pair<String, Long>> =
        usageBetween(context, start, System.currentTimeMillis())
            .filterValues { it > 0L }
            .toList()
            .sortedByDescending { it.second }
}
