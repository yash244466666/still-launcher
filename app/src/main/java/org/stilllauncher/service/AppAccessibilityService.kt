package org.stilllauncher.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.stilllauncher.data.SettingsStore
import org.stilllauncher.util.Diagnostics
import org.stilllauncher.util.HomeGestureGuard

class AppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastPackage: String? = null
    private var lastCheckAt = 0L
    private var lastWindowKey: String? = null
    private var lastWindowPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MonitorService.start(this)
        scope.launch {
            SettingsStore.get(this@AppAccessibilityService).settings.collect {
                HomeGestureGuard.setEnabled(it.homeGestureGuard)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString().orEmpty()
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val key = "$pkg/$className"
            if (key != lastWindowKey) {
                val cameFromLauncher = lastWindowPackage == packageName
                lastWindowKey = key
                lastWindowPackage = pkg
                Diagnostics.record("window", key)
                if (HomeGestureGuard.shouldReturnHome(
                        this,
                        pkg,
                        className,
                        cameFromLauncher
                    )
                ) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    return
                }
                HomeGestureGuard.onWindowShown(pkg, className, packageName)
            }
        }
        val now = SystemClock.elapsedRealtime()
        if (pkg != lastPackage || now - lastCheckAt > 700L) {
            lastPackage = pkg
            lastCheckAt = now
            MonitorEngine.onForeground(this, pkg)
            if (MonitorEngine.snapshot().shortVideoBlocking && ShortVideoDetector.matches(pkg)) {
                val root = runCatching { rootInActiveWindow }.getOrNull()
                if (ShortVideoDetector.isShowing(pkg, root)) {
                    MonitorEngine.interruptShortVideo(this, pkg)
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: AppAccessibilityService? = null
            private set

        fun expandNotifications(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false

        fun lockScreen(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false

        fun openRecents(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
    }
}
