package org.stilllauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import org.stilllauncher.data.UsageRepository

object Permissions {

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun hasUsageAccess(context: Context): Boolean = UsageRepository.hasAccess(context)

    fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
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
