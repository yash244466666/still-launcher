package org.stilllauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import androidx.core.net.toUri
import java.util.Locale

class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps? =
        context.getSystemService(LauncherApps::class.java)

    fun loadApps(): List<LauncherApp> {
        val myUser = Process.myUserHandle()
        val profiles = runCatching { launcherApps?.profiles }.getOrNull().orEmpty()
            .ifEmpty {
                runCatching {
                    context.getSystemService(UserManager::class.java)?.userProfiles
                }.getOrNull().orEmpty()
            }
            .ifEmpty { listOf(myUser) }
            .distinct()

        val fromLauncher = profiles.flatMap { user ->
            runCatching { launcherApps?.getActivityList(null, user) }
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
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            userHandle = user,
                            profileKey = profileKeyFor(pkg, user.hashCode(), myUser.hashCode()),
                            profileSuffix = profileSuffixFor(user, myUser)
                        )
                    }.getOrNull()
                }
        }

        val apps = if (fromLauncher.isNotEmpty()) {
            fromLauncher
        } else {
            // Fallback for devices that do not expose LauncherApps profiles.
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
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            userHandle = myUser,
                            profileKey = pkg,
                            profileSuffix = ""
                        )
                    }.getOrNull()
                }
            }.getOrDefault(emptyList())
        }

        return apps.distinctBy { it.profileKey }
            .filterNot { it.packageName == context.packageName }
            .sortedWith(
                compareBy(
                    { it.label.lowercase(Locale.getDefault()) },
                    { if (it.isCurrentProfile) 0 else 1 }
                )
            )
    }

    private fun profileSuffixFor(user: UserHandle, myUser: UserHandle): String {
        if (user == myUser) return ""
        // OEM "dual apps", "parallel apps" and work profiles are all separate
        // Android users. Android does not expose a reliable public per-profile
        // type through LauncherApps, so use one clear label.
        return " (dual app)"
    }

    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    }.recoverCatching {
        // PackageManager visibility can hide another launcher from us. The
        // LauncherApps service still knows about launchable packages.
        val launcherApp = launcherApps?.getApplicationInfo(
            packageName,
            0,
            android.os.Process.myUserHandle()
        ) ?: throw IllegalStateException("not visible")
        context.packageManager.getApplicationLabel(launcherApp).toString()
    }.getOrDefault(packageName)

    fun launch(app: LauncherApp) {
        runCatching {
            launcherApps?.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                app.userHandle,
                null,
                null
            )
        }.onFailure {
            if (app.isCurrentProfile) launchPackage(app.packageName)
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

    fun openAppInfo(app: LauncherApp) {
        if (app.isCurrentProfile) {
            openAppInfo(app.packageName)
            return
        }
        runCatching {
            launcherApps?.startAppDetailsActivity(
                ComponentName(app.packageName, app.activityName),
                app.userHandle,
                null,
                null
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

    fun uninstall(app: LauncherApp) {
        if (app.isCurrentProfile) uninstall(app.packageName)
    }
}
