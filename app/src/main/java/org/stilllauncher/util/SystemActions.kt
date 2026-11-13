package org.stilllauncher.util

import android.content.Context
import android.content.Intent
import org.stilllauncher.service.AppAccessibilityService

object SystemActions {

    fun expandNotifications(context: Context): Boolean {
        if (AppAccessibilityService.expandNotifications()) return true
        return runCatching {
            val service = context.getSystemService("statusbar")
            val cls = Class.forName("android.app.StatusBarManager")
            val method = cls.getMethod("expandNotificationsPanel")
            method.invoke(service)
            true
        }.getOrDefault(false)
    }

    fun goHome(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun lockScreen(): Boolean = AppAccessibilityService.lockScreen()
}
