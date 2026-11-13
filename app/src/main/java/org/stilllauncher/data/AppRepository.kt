package org.stilllauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.provider.Settings
import androidx.core.net.toUri
import java.util.Locale

class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps? =
        context.getSystemService(LauncherApps::class.java)

    fun loadApps(): List<LauncherApp> {
        val user = Process.myUserHandle()
        val fromLauncher = runCatching { launcherApps?.getActivityList(null, user) }
            .getOrNull()
            .orEmpty()
            .mapNotNull { info ->
                runCatching {
                    val pkg = info.componentName.packageName
                    LauncherApp(
                        packageName = pkg,
                        activityName = info.componentName.className,
                        label = info.label?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: pkg,
                        isSystem = (info.applicationInfo.flags and
                            android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }.getOrNull()
            }

        val apps = if (fromLauncher.isNotEmpty()) {
            fromLauncher
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            runCatching {
                context.packageManager.queryIntentActivities(intent, 0).mapNotNull { info ->
                    runCatching {
                        val pkg = info.activityInfo.packageName
                        LauncherApp(
                            packageName = pkg,
                            activityName = info.activityInfo.name,
                            label = info.loadLabel(context.packageManager).toString().trim()
                                .ifEmpty { pkg },
                            isSystem = (info.activityInfo.applicationInfo.flags and
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }.getOrNull()
                }
            }.getOrDefault(emptyList())
        }

        return apps.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun launch(app: LauncherApp) {
        runCatching {
            launcherApps?.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                Process.myUserHandle(),
                null,
                null
            )
        }.onFailure {
            launchPackage(app.packageName)
        }
    }

    fun launchPackage(packageName: String) {
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openAppInfo(packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun uninstall(packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE)
                    .setData("package:$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
