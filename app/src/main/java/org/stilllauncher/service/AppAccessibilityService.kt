package org.stilllauncher.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class AppAccessibilityService : AccessibilityService() {

    private var lastPackage: String? = null
    private var lastCheckAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MonitorService.start(this)
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
    }
}
