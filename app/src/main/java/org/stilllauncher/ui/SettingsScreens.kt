package org.stilllauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.Settings
import org.stilllauncher.util.Formats
import org.stilllauncher.util.Permissions

@Composable
fun SettingsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }

    ScreenScaffold(
        title = "Settings",
        onBack = onBack,
        trailing = {
            TextButton(onClick = { refresh++ }) { Text("Refresh") }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Status")
            SettingRow(
                "Permissions",
                subtitle = permissionSummary(context, refresh),
                onClick = { onNavigate(Route.Permissions) }
            )
            SettingRow(
                "Default home app",
                subtitle = if (Permissions.isDefaultLauncher(context)) "Still Launcher" else "Not selected",
                onClick = { Permissions.openHomeSettings(context) }
            )
            SwitchRow(
                title = "Background focus guard",
                subtitle = "Keeps blocks, limits and grayscale working",
                checked = settings.monitorEnabled,
                onCheckedChange = viewModel::setMonitorEnabled
            )

            SectionLabel("Home screen")
            SettingRow(
                "Theme",
                subtitle = themeLabel(settings.themeId),
                onClick = { onNavigate(Route.Theme) }
            )
            SettingRow(
                "Text and font",
                subtitle = "${fontLabel(settings.fontId)} · ${settings.fontSizeSp} sp",
                onClick = { onNavigate(Route.TextAndFont) }
            )
            SettingRow(
                "Date and time",
                subtitle = "${timeLabel(settings.timeFormat)} · ${dateLabel(settings.dateFormat)}",
                onClick = { onNavigate(Route.DateTime) }
            )
            SettingRow(
                "Gestures",
                subtitle = gestureSummary(settings),
                onClick = { onNavigate(Route.Gestures) }
            )
            SettingRow(
                "Hidden apps",
                subtitle = "${settings.hidden.size} hidden",
                onClick = { onNavigate(Route.HiddenApps) }
            )

            SectionLabel("Focus")
            SettingRow(
                "Blocking schedules",
                subtitle = "${settings.schedules.size} schedule(s)",
                onClick = { onNavigate(Route.Schedules) }
            )
            SettingRow(
                "Daily app limits",
                subtitle = "${settings.limits.size} app(s)",
                onClick = { onNavigate(Route.Limits) }
            )
            SettingRow(
                "Monochrome mode",
                subtitle = if (settings.monochromeEnabled) {
                    "${settings.monochromePackages.size} app(s) in grayscale"
                } else "Off",
                onClick = { onNavigate(Route.Monochrome) }
            )
            SettingRow(
                "Notification filter",
                subtitle = if (settings.notificationManagerActive) {
                    if (settings.notificationAllowed.isEmpty()) "Active · all allowed"
                    else "Active · ${settings.notificationAllowed.size} allowed"
                } else "Off",
                onClick = { onNavigate(Route.Notifications) }
            )
            SwitchRow(
                title = "Block short videos",
                subtitle = "Interrupt Reels, Shorts and Spotlight while scrolling (experimental)",
                checked = settings.shortVideoBlocking,
                onCheckedChange = viewModel::setShortVideoBlocking
            )
            SettingRow(
                "Screen time",
                subtitle = "Usage on this device",
                onClick = { onNavigate(Route.ScreenTime) }
            )

            SectionLabel("About")
            SettingRow(
                "About Still Launcher",
                subtitle = "Open source · version 0.1.0",
                onClick = { onNavigate(Route.About) }
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun ThemeScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    val palettes = listOf(
        0xFF264653, 0xFF3ECCBB, 0xFF8EE1D7, 0xFFE9C46A,
        0xFFF4A261, 0xFFE76F51, 0xFFB8C1EC, 0xFFF7D6E0,
        0xFF1A1A1A, 0xFFF4F1EA
    )
    ScreenScaffold("Theme", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            listOf(
                "system" to "Follow system",
                "black" to "Black",
                "white" to "White",
                "eink" to "E-Ink",
                "custom" to "Custom"
            ).forEach { (id, label) ->
                OptionRow(
                    label = label,
                    selected = settings.themeId == id,
                    onClick = { viewModel.setThemeId(id) }
                )
            }

            SectionLabel("Custom colour")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                palettes.take(5).forEach { color ->
                    ColorDot(color, selected = settings.themeId == "custom" && settings.customColor == color) {
                        viewModel.setCustomColor(color)
                        viewModel.setThemeId("custom")
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                palettes.drop(5).forEach { color ->
                    ColorDot(color, selected = settings.themeId == "custom" && settings.customColor == color) {
                        viewModel.setCustomColor(color)
                        viewModel.setThemeId("custom")
                    }
                }
            }
            SwitchRow(
                "Use a dark custom theme",
                "Switches text and controls to a light-on-dark palette",
                settings.customDark,
                viewModel::setCustomDark
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorDot(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .background(Color(color.toInt()), CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun TextAndFontScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    val fonts = listOf(
        "default" to "Default",
        "serif" to "Serif",
        "monospace" to "Monospace",
        "cursive" to "Handwriting"
    )
    val sizes = listOf(12, 14, 16, 18, 20, 24, 28)
    ScreenScaffold("Text and font", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Font")
            fonts.forEach { (id, label) ->
                OptionRow(label, settings.fontId == id) { viewModel.setFontId(id) }
            }
            SectionLabel("Size")
            sizes.forEach { size ->
                OptionRow("$size sp", settings.fontSizeSp == size) {
                    viewModel.setFontSizeSp(size)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "The quick brown fox jumps over the lazy dog.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DateTimeScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    ScreenScaffold("Date and time", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Clock")
            listOf(
                "system" to "System default",
                "24" to "24-hour",
                "12" to "12-hour"
            ).forEach { (id, label) ->
                OptionRow(label, settings.timeFormat == id) { viewModel.setTimeFormat(id) }
            }
            SectionLabel("Date")
            listOf(
                "system" to "System default",
                "iso" to "2026-11-13",
                "dmy" to "13/11/2026",
                "mdy" to "Nov 13, 2026"
            ).forEach { (id, label) ->
                OptionRow(label, settings.dateFormat == id) { viewModel.setDateFormat(id) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun GesturesScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    ScreenScaffold("Gestures", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Home screen")
            SwitchRow(
                "Swipe up to search",
                "Open the app list and search",
                settings.swipeUpSearch,
                viewModel::setSwipeUpSearch
            )
            SwitchRow(
                "Swipe down for notifications",
                "Expand the notification shade",
                settings.swipeDownNotifications,
                viewModel::setSwipeDownNotifications
            )
            SwitchRow(
                "Double-tap to lock",
                "Requires the accessibility service",
                settings.doubleTapLock,
                viewModel::setDoubleTapLock
            )
            SectionLabel("App list")
            SwitchRow(
                "Open the keyboard automatically",
                "Focus the search field when opening the app list",
                settings.autoKeyboard,
                viewModel::setAutoKeyboard
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold("About", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Still Launcher", style = MaterialTheme.typography.titleLarge)
            Text("Version 0.1.0", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text(
                "An open-source, text-first Android launcher for people who want fewer icons and more intention. It is an independent clean-room implementation inspired by the ideas behind minimalist launchers; it shares no code, assets or branding with any commercial product.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Privacy\n\nNo account. No analytics. No ads. No network permission is requested. App lists, blocks, limits, notification settings and usage-derived statistics stay in this app's private storage. Usage access and the accessibility service are used only to know which app is currently in the foreground.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Licence\n\nSource code is released under the GNU General Public License v3.0. The app icon and all text in this project are original.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun PermissionsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }
    val notificationsGranted = android.os.Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

    ScreenScaffold(
        title = "Permissions",
        onBack = onBack,
        trailing = { TextButton(onClick = { refresh++ }) { Text("Refresh") } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "All of these are optional. Blocks and limits need usage access, the accessibility service, or both. Nothing leaves the device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            PermissionRow(
                title = "Default home app",
                granted = Permissions.isDefaultLauncher(context),
                actionLabel = "Choose",
                onAction = { Permissions.openHomeSettings(context) }
            )
            PermissionRow(
                title = "Usage access",
                granted = Permissions.hasUsageAccess(context),
                actionLabel = "Grant",
                onAction = { Permissions.openUsageAccess(context) }
            )
            PermissionRow(
                title = "Accessibility service",
                granted = Permissions.isAccessibilityEnabled(context),
                actionLabel = "Enable",
                onAction = { Permissions.openAccessibility(context) }
            )
            PermissionRow(
                title = "Notification listener",
                granted = Permissions.isNotificationListenerEnabled(context),
                actionLabel = "Enable",
                onAction = { Permissions.openNotificationListener(context) }
            )
            PermissionRow(
                title = "Post notifications",
                granted = notificationsGranted,
                actionLabel = "Allow",
                onAction = {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            PermissionRow(
                title = "Display over other apps",
                granted = Permissions.canDrawOverlays(context),
                actionLabel = "Allow",
                onAction = { Permissions.openOverlaySettings(context) }
            )
            PermissionRow(
                title = "Ignore battery optimisation",
                granted = Permissions.isIgnoringBatteryOptimizations(context),
                actionLabel = "Allow",
                onAction = { Permissions.openBatteryOptimization(context) }
            )
            PermissionRow(
                title = "Write secure settings (monochrome)",
                granted = Permissions.canWriteSecureSettings(context),
                actionLabel = "Instructions",
                onAction = { copied = false }
            )

            if (!Permissions.canWriteSecureSettings(context)) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Grayscale needs a permission that Android only grants from a computer. Connect this phone with USB debugging enabled and run:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(
                                "adb command",
                                "adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS"
                            )
                        )
                        copied = true
                    }) { Text(if (copied) "Copied" else "Copy command") }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "If a permission is revoked, the focus guard simply stops enforcing blocks until it is granted again. Force-stopping the app also disables enforcement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) "Granted" else "Not granted",
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            if (selected) "●" else "○",
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun permissionSummary(context: Context, refresh: Int): String {
    val granted = buildList {
        if (Permissions.hasUsageAccess(context)) add("usage")
        if (Permissions.isAccessibilityEnabled(context)) add("accessibility")
        if (Permissions.isNotificationListenerEnabled(context)) add("notifications")
    }
    return if (granted.isEmpty()) "Not granted · tap to review"
    else granted.joinToString(", ")
}

private fun themeLabel(id: String): String = when (id) {
    "black" -> "Black"
    "white" -> "White"
    "eink" -> "E-Ink"
    "custom" -> "Custom"
    else -> "Follow system"
}

private fun fontLabel(id: String): String = when (id) {
    "serif" -> "Serif"
    "monospace" -> "Monospace"
    "cursive" -> "Handwriting"
    else -> "Default"
}

private fun timeLabel(id: String): String = when (id) {
    "24" -> "24-hour"
    "12" -> "12-hour"
    else -> "System"
}

private fun dateLabel(id: String): String = when (id) {
    "iso" -> "YYYY-MM-DD"
    "dmy" -> "DD/MM/YYYY"
    "mdy" -> "Mon D, YYYY"
    else -> "System"
}

private fun gestureSummary(settings: Settings): String {
    val enabled = buildList {
        if (settings.swipeUpSearch) add("swipe up")
        if (settings.swipeDownNotifications) add("swipe down")
        if (settings.doubleTapLock) add("double tap")
    }
    return if (enabled.isEmpty()) "All off" else enabled.joinToString(", ")
}
