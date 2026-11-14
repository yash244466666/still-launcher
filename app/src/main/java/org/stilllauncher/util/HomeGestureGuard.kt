package org.stilllauncher.util

import android.content.Context
import android.os.SystemClock
import java.util.Locale

/**
 * Countermeasure for OEM gesture implementations that open the system
 * overview when the user swipes Home while our home screen is already
 * showing. Infinix/XOS (and other Transsion builds) can draw the launcher
 * task as an empty card in that overview, which looks like a blank screen
 * containing only the wallpaper.
 *
 * When the guard is enabled and an overview/recents window appears within a
 * short window after our home activity was the focused app, we send HOME
 * again. Android then closes the overview and returns to the launcher. The
 * Recents button in the dock suppresses the guard so deliberate access to the
 * overview keeps working.
 */
object HomeGestureGuard {

    private const val LOOP_BREAKER_MS = 1_500L

    @Volatile private var enabled = false
    @Volatile private var homeVisible = false
    @Volatile private var suppressUntil = 0L

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun onHomeResumed() {
        homeVisible = true
    }

    /**
     * Called for every distinct window. A non-launcher window means the user
     * has moved on to an app, so a later Recents window is a deliberate
     * switch and must not be dismissed.
     */
    fun onWindowShown(packageName: String, className: String, launcherPackage: String) {
        if (packageName == launcherPackage) return
        if (isOverviewWindow(packageName, className)) return
        val pkg = packageName.lowercase(Locale.US)
        // System overlays (notification shade, volume panel, permission
        // dialogs) do not mean the user left the home screen.
        if (pkg.contains("systemui") ||
            pkg.contains("permissioncontroller") ||
            pkg == "android"
        ) {
            return
        }
        homeVisible = false
    }

    /** Keeps the guard quiet while Recents is opened from our own dock. */
    fun suppress(durationMs: Long = 2_500L) {
        suppressUntil = SystemClock.elapsedRealtime() + durationMs
    }

    /**
     * @param cameFromLauncher true when the window shown immediately before
     * this one belonged to Still Launcher. The accessibility service supplies
     * this from the last distinct window event, so opening Recents from
     * inside another app is never mistaken for the blank home overview.
     */
    fun shouldReturnHome(
        context: Context,
        packageName: String,
        className: String,
        cameFromLauncher: Boolean
    ): Boolean {
        if (!enabled) return false
        if (SystemClock.elapsedRealtime() < suppressUntil) return false
        if (!homeVisible && !cameFromLauncher) return false
        if (!isOverviewWindow(packageName, className)) return false
        if (!Permissions.isDefaultLauncher(context)) return false
        suppress(LOOP_BREAKER_MS)
        Diagnostics.record("guard", "overview detected: $packageName/$className")
        return true
    }

    private fun isOverviewWindow(packageName: String, className: String): Boolean {
        val pkg = packageName.lowercase(Locale.US)
        val cls = className.lowercase(Locale.US)
        if (cls.contains("recents") || cls.contains("overview")) return true
        return pkg == "com.android.systemui" && cls.contains("recent")
    }
}
