package org.stilllauncher.util

import android.Manifest
import android.app.role.RoleManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import org.stilllauncher.data.UsageRepository
import org.stilllauncher.service.AppAccessibilityService

data class PermissionStatus(
    val defaultLauncher: Boolean,
    val usageAccess: Boolean,
    val accessibility: Boolean,
    val notificationListener: Boolean,
    val postNotifications: Boolean,
    val overlay: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val secureSettings: Boolean
)

object Permissions {

    /**
     * Reads every permission once. UI screens should call this behind a
     * refresh counter so a manual grant, a system-settings round trip or the
     * Refresh button updates the displayed state.
     */
    fun readStatus(context: Context): PermissionStatus {
        val postNotifications = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return PermissionStatus(
            defaultLauncher = isDefaultLauncher(context),
            usageAccess = hasUsageAccess(context),
            accessibility = isAccessibilityEnabled(context),
            notificationListener = isNotificationListenerEnabled(context),
            postNotifications = postNotifications,
            overlay = canDrawOverlays(context),
            ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            secureSettings = canWriteSecureSettings(context)
        )
    }

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) return true
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun hasUsageAccess(context: Context): Boolean {
        if (UsageRepository.hasAccess(context)) return true
        // Some OEMs report a non-standard app-op mode. A non-empty usage query
        // is a practical fallback that only succeeds with access.
        return runCatching {
            val manager = context.getSystemService(UsageStatsManager::class.java)
                ?: return@runCatching false
            val now = System.currentTimeMillis()
            manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 86_400_000L,
                now
            )?.isNotEmpty() == true
        }.getOrDefault(false)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val fromManager = manager?.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )?.any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        } == true
        if (fromManager) return true

        // AccessibilityManager can keep a stale list immediately after the user
        // returns from system settings; read the secure entry as a fallback.
        val resolver = context.contentResolver
        val enabledServices = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val listed = enabledServices.split(':').any { value ->
            val component = ComponentName.unflattenFromString(value) ?: return@any false
            component.packageName == context.packageName &&
                component.className == AppAccessibilityService::class.java.name
        }
        if (!listed) return false
        val master = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        return master == 1 || manager?.isEnabled == true
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        if (NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        ) {
            return true
        }
        // Fallback for OEMs where the compat cache lags behind the setting.
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.split(':').any { value ->
            ComponentName.unflattenFromString(value)?.packageName == context.packageName
        }
    }

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun canDrawOverlays(context: Context): Boolean =
        android.provider.Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        safeStart(
            context,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openUsageAccess(context: Context) {
        safeStart(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openAccessibility(context: Context) {
        safeStart(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openNotificationListener(context: Context) {
        safeStart(context, Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openHomeSettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_HOME_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBatteryOptimization(context: Context) {
        // Open the system list instead of requesting an exemption directly. This
        // avoids a Play-policy-sensitive permission and keeps the choice with the user.
        safeStart(
            context,
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAppSettings(context: Context) {
        safeStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun safeStart(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent) }
    }
}
