package org.stilllauncher.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.stilllauncher.data.ReminderBehavior
import org.stilllauncher.data.Settings
import org.stilllauncher.data.SettingsStore
import org.stilllauncher.data.UsageRepository
import org.stilllauncher.ui.BlockedActivity
import org.stilllauncher.ui.ReminderActivity

/**
 * Single place where foreground transitions are turned into enforcement.
 * Both the accessibility service and the 2-second UsageStats poll feed it.
 */
object MonitorEngine {

    private const val ACTION_DEBOUNCE_MS = 2_500L
    private const val REMIND_COOLDOWN_MS = 10 * 60_000L
    private const val COOLDOWN_MS = 15 * 60_000L

    @Volatile private var settings: Settings = Settings()
    @Volatile private var store: SettingsStore? = null
    @Volatile private var scope: CoroutineScope? = null
    @Volatile private var lastMonochrome: Boolean? = null

    private var lastActionPackage: String? = null
    private var lastActionAt = 0L

    fun attach(store: SettingsStore, scope: CoroutineScope) {
        this.store = store
        this.scope = scope
    }

    fun updateSettings(value: Settings) {
        settings = value
    }

    fun snapshot(): Settings = settings

    fun onForeground(context: Context, packageName: String?) {
        val pkg = packageName ?: return
        if (pkg == "com.android.systemui") return
        applyMonochrome(context, pkg)
        if (pkg == context.packageName) return
        if (pkg == "android") return

        val now = System.currentTimeMillis()

        val direct = directBlock(pkg, now)
        if (direct != null) {
            if (shouldAct(pkg, now)) {
                launchBlocked(context, pkg, direct.blockedUntil, direct.reason)
            }
            return
        }

        val schedule = settings.schedules.firstOrNull {
            it.isActiveAt(java.time.LocalDateTime.now()) && pkg in it.packages
        }
        if (schedule != null) {
            if (shouldAct(pkg, now)) {
                launchBlocked(context, pkg, schedule.nextEndMillis(), schedule.name)
            }
            return
        }

        if (!settings.reminderMaster) return
        val limit = settings.limits.firstOrNull { it.packageName == pkg } ?: return

        if (limit.cooldownUntil > now) {
            if (shouldAct(pkg, now)) {
                launchBlocked(context, pkg, limit.cooldownUntil, "Daily limit reached")
            }
            return
        }

        val used = UsageRepository.usageSince(context, pkg, UsageRepository.startOfToday())
        val limitMs = limit.minutes * 60_000L
        if (used < limitMs) return
        if (now - limit.lastRemindedAt < REMIND_COOLDOWN_MS) return

        if (limit.behavior == ReminderBehavior.COOLDOWN) {
            val updated = settings.limits.map {
                if (it.packageName == pkg) {
                    it.copy(lastRemindedAt = now, cooldownUntil = now + COOLDOWN_MS)
                } else it
            }
            settings = settings.copy(limits = updated)
            persistLimits(updated)
            launchBlocked(context, pkg, now + COOLDOWN_MS, "Daily limit reached")
        } else {
            val updated = settings.limits.map {
                if (it.packageName == pkg) it.copy(lastRemindedAt = now) else it
            }
            settings = settings.copy(limits = updated)
            persistLimits(updated)
            launchReminder(context, pkg, used, limit.minutes, limit.behavior)
        }
    }

    fun interruptShortVideo(context: Context, packageName: String) {
        val now = System.currentTimeMillis()
        if (shouldAct(packageName, now)) {
            launchBlocked(context, packageName, now + 4_000L, "Short videos are paused")
        }
    }

    fun clearMonochrome(context: Context) {
        lastMonochrome = null
        runCatching {
            val cr = context.contentResolver
            android.provider.Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 0)
            android.provider.Settings.Secure.putInt(cr, "accessibility_display_daltonizer", -1)
        }
    }

    private fun applyMonochrome(context: Context, pkg: String) {
        val target = settings.monochromeEnabled && pkg in settings.monochromePackages
        if (lastMonochrome == target) return
        val ok = runCatching {
            val cr = context.contentResolver
            android.provider.Settings.Secure.putInt(
                cr,
                "accessibility_display_daltonizer_enabled",
                if (target) 1 else 0
            )
            android.provider.Settings.Secure.putInt(
                cr,
                "accessibility_display_daltonizer",
                if (target) 0 else -1
            )
            true
        }.getOrDefault(false)
        if (ok) lastMonochrome = target
    }

    private data class DirectBlock(val blockedUntil: Long, val reason: String)

    private fun directBlock(pkg: String, now: Long): DirectBlock? {
        val entry = settings.blocked.firstOrNull {
            it.packageName == pkg && it.blockedUntil > now
        } ?: return null
        return DirectBlock(entry.blockedUntil, "Blocked")
    }

    private fun shouldAct(pkg: String, now: Long): Boolean {
        if (pkg == lastActionPackage && now - lastActionAt < ACTION_DEBOUNCE_MS) return false
        lastActionPackage = pkg
        lastActionAt = now
        return true
    }

    private fun persistLimits(list: List<org.stilllauncher.data.AppLimit>) {
        val s = store ?: return
        val sc = scope ?: return
        sc.launch(kotlinx.coroutines.Dispatchers.IO) {
            s.setLimits(list)
        }
    }

    private fun launchBlocked(context: Context, pkg: String, until: Long, reason: String) {
        runCatching {
            context.startActivity(
                Intent(context, BlockedActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    .putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
                    .putExtra(BlockedActivity.EXTRA_UNTIL, until)
                    .putExtra(BlockedActivity.EXTRA_REASON, reason)
            )
        }
    }

    private fun launchReminder(
        context: Context,
        pkg: String,
        used: Long,
        limitMinutes: Int,
        behavior: ReminderBehavior
    ) {
        runCatching {
            context.startActivity(
                Intent(context, ReminderActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    .putExtra(ReminderActivity.EXTRA_PACKAGE, pkg)
                    .putExtra(ReminderActivity.EXTRA_USED, used)
                    .putExtra(ReminderActivity.EXTRA_LIMIT, limitMinutes)
                    .putExtra(ReminderActivity.EXTRA_BEHAVIOR, behavior.name)
            )
        }
    }
}
