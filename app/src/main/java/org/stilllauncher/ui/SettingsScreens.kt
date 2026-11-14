package org.stilllauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.stilllauncher.BuildConfig
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.Settings
import org.stilllauncher.util.Diagnostics
import org.stilllauncher.util.Formats
import org.stilllauncher.util.PermissionStatus
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
    var clearRecents by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            refresh++
        }
    }
    val status = remember(refresh) { Permissions.readStatus(context) }

    ScreenScaffold(
        title = "Settings",
        onBack = onBack,
        subtitle = "Private, on-device, and yours to change",
        trailing = {
            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh status")
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SetupOverviewCard(status) { onNavigate(Route.Permissions) }

            SectionHeader(
                "Focus tools",
                "Rules that help you use your phone on purpose",
                StillIcons.Shield
            )
            SettingsCard {
                ToggleRow(
                    icon = StillIcons.Shield,
                    title = "Focus guard",
                    subtitle = "Keeps blocks, limits and grayscale working in the background",
                    checked = settings.monitorEnabled,
                    onCheckedChange = viewModel::setMonitorEnabled
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.DateRange,
                    title = "Blocking schedules",
                    subtitle = schedulesSummary(settings),
                    onClick = { onNavigate(Route.Schedules) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Timer,
                    title = "Daily app limits",
                    subtitle = limitsSummary(settings),
                    onClick = { onNavigate(Route.Limits) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Contrast,
                    title = "Monochrome mode",
                    subtitle = monochromeSummary(settings),
                    onClick = { onNavigate(Route.Monochrome) }
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification filter",
                    subtitle = notificationSummary(settings),
                    onClick = { onNavigate(Route.Notifications) }
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Outlined.PlayArrow,
                    title = "Block short videos",
                    subtitle = "Interrupt Reels, Shorts and Spotlight while scrolling (experimental)",
                    checked = settings.shortVideoBlocking,
                    onCheckedChange = viewModel::setShortVideoBlocking
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Chart,
                    title = "Screen time",
                    subtitle = "See where your time goes on this device",
                    onClick = { onNavigate(Route.ScreenTime) }
                )
            }

            SectionHeader(
                "Home screen",
                "How the launcher looks and behaves",
                Icons.Outlined.Home
            )
            SettingsCard {
                NavRow(
                    icon = StillIcons.Palette,
                    title = "Theme",
                    subtitle = themeLabel(settings.themeId),
                    onClick = { onNavigate(Route.Theme) }
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Create,
                    title = "Text and font",
                    subtitle = "${fontLabel(settings.fontId)} · ${settings.fontSizeSp} sp",
                    onClick = { onNavigate(Route.TextAndFont) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Clock,
                    title = "Date and time",
                    subtitle = "${timeLabel(settings.timeFormat)} · ${dateLabel(settings.dateFormat)}",
                    onClick = { onNavigate(Route.DateTime) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Gesture,
                    title = "Gestures",
                    subtitle = gestureSummary(settings),
                    onClick = { onNavigate(Route.Gestures) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.EyeOff,
                    title = "Hidden apps",
                    subtitle = hiddenSummary(settings),
                    onClick = { onNavigate(Route.HiddenApps) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.History,
                    title = "Recent apps",
                    subtitle = recentsSummary(settings),
                    onClick = { clearRecents = true }
                )
            }

            SectionHeader("Setup & help")
            SettingsCard {
                NavRow(
                    icon = StillIcons.Shield,
                    title = "Permissions",
                    subtitle = permissionSummary(status),
                    onClick = { onNavigate(Route.Permissions) },
                    trailingText = if (status.essentialsReady) "Ready" else "Review"
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Home,
                    title = "Default home app",
                    subtitle = if (status.defaultLauncher) "Still Launcher" else "Not selected yet",
                    onClick = { Permissions.openHomeSettings(context) }
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Warning,
                    title = "Install & access help",
                    subtitle = "Play Protect warnings and restricted settings",
                    onClick = { onNavigate(Route.InstallHelp) }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Gesture,
                    title = "Home gesture diagnostics",
                    subtitle = "Window events from the last few minutes",
                    onClick = { onNavigate(Route.Diagnostics) }
                )
            }

            SectionHeader("About")
            SettingsCard {
                NavRow(
                    icon = Icons.Outlined.Info,
                    title = "About Still Launcher",
                    subtitle = "Open source · version ${BuildConfig.VERSION_NAME}",
                    onClick = { onNavigate(Route.About) }
                )
            }
            Text(
                "No account · no analytics · no network permission",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            )
            Spacer(Modifier.height(30.dp))
        }
    }

    if (clearRecents) {
        ConfirmDialog(
            title = "Clear recent apps?",
            message = "This only removes the local history used for the recent-app shortcut. It does not affect the apps themselves.",
            confirmLabel = "Clear",
            destructive = true,
            icon = StillIcons.History,
            onDismiss = { clearRecents = false },
            onConfirm = { viewModel.clearRecentApps() }
        )
    }
}

@Composable
private fun SetupOverviewCard(status: PermissionStatus, onReview: () -> Unit) {
    val essentials = listOf(status.defaultLauncher, status.usageAccess, status.accessibility)
    val ready = essentials.count { it }
    val allReady = ready == essentials.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (allReady) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(if (allReady) Icons.Outlined.CheckCircle else StillIcons.Shield)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (allReady) "You're all set" else "Finish setting up",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (allReady) {
                            "All essential access is on. Your rules can work."
                        } else {
                            "$ready of 3 essentials ready. Blocks, limits and the background app indicator need them."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    if (allReady) "Ready" else "$ready/3",
                    if (allReady) StatusTone.Positive else StatusTone.Warning
                )
            }
            if (!allReady) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onReview,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Review permissions")
                }
            }
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
    ScreenScaffold("Theme", onBack, subtitle = "A calm look that stays out of the way") {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))
            ThemePreview()
            SectionHeader("Appearance")
            SettingsCard {
                listOf(
                    Triple("system", "Follow system", "Matches your phone's light or dark mode"),
                    Triple("black", "Black", "Pure black, for OLED screens"),
                    Triple("white", "White", "Bright, paper-like background"),
                    Triple("eink", "E-Ink", "Soft grey with low contrast"),
                    Triple("custom", "Custom", "Choose your own background colour")
                ).forEachIndexed { index, (id, label, description) ->
                    if (index > 0) RowDivider(startInset = 14.dp)
                    ChoiceRow(
                        title = label,
                        description = description,
                        selected = settings.themeId == id,
                        onClick = { viewModel.setThemeId(id) }
                    )
                }
            }

            SectionHeader("Custom colour", "Tap a swatch to use it")
            SettingsCard {
                Column(Modifier.padding(14.dp)) {
                    palettes.chunked(5).forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEach { color ->
                                ColorDot(
                                    color = color,
                                    selected = settings.themeId == "custom" &&
                                        settings.customColor == color
                                ) {
                                    viewModel.setCustomColor(color)
                                    viewModel.setThemeId("custom")
                                }
                            }
                        }
                    }
                }
                if (settings.themeId == "custom") {
                    RowDivider(startInset = 14.dp)
                    ToggleRow(
                        icon = StillIcons.Contrast,
                        title = "Dark custom theme",
                        subtitle = "Use light text and controls on your colour",
                        checked = settings.customDark,
                        onCheckedChange = viewModel::setCustomDark
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemePreview() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("A quieter home screen", style = MaterialTheme.typography.titleLarge)
            Text(
                "Plain words, calm colours, fewer distractions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = true, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                StatusPill("On", StatusTone.Positive)
                Spacer(Modifier.width(8.dp))
                StatusPill("Optional", StatusTone.Neutral)
            }
        }
    }
}

@Composable
private fun ColorDot(color: Long, selected: Boolean, onClick: () -> Unit) {
    val dot = Color(color.toInt())
    Box(
        Modifier
            .size(46.dp)
            .background(dot, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Selected colour",
                tint = if (dot.luminance() > 0.45f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TextAndFontScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    var sizeValue by remember(settings.fontSizeSp) {
        mutableFloatStateOf(settings.fontSizeSp.toFloat())
    }
    LaunchedEffect(settings.fontSizeSp) {
        sizeValue = settings.fontSizeSp.toFloat()
    }
    ScreenScaffold("Text and font", onBack, subtitle = "Applies everywhere in Still Launcher") {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("The quick brown fox", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "jumps over the lazy dog. Your app names and settings use this style.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            SectionHeader("Font")
            SettingsCard {
                listOf(
                    "default" to "Default",
                    "serif" to "Serif",
                    "monospace" to "Monospace",
                    "cursive" to "Handwriting"
                ).forEachIndexed { index, (id, label) ->
                    if (index > 0) RowDivider(startInset = 14.dp)
                    ChoiceRow(
                        title = label,
                        selected = settings.fontId == id,
                        onClick = { viewModel.setFontId(id) }
                    )
                }
            }

            SectionHeader("Size")
            SettingsCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Text size", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            "${sizeValue.roundToInt()} sp",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sizeValue,
                        onValueChange = { sizeValue = it },
                        onValueChangeFinished = {
                            viewModel.setFontSizeSp(sizeValue.roundToInt())
                        },
                        valueRange = 12f..28f,
                        steps = 7
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Small", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Large", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DateTimeScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    ScreenScaffold("Date and time", onBack, subtitle = "How the clock looks on your home screen") {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Formats.time(settings, now),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.6f
                        )
                    )
                    Text(
                        Formats.date(settings, now.toLocalDate()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionHeader("Clock")
            SettingsCard {
                listOf(
                    "system" to "System default",
                    "24" to "24-hour",
                    "12" to "12-hour"
                ).forEachIndexed { index, (id, label) ->
                    if (index > 0) RowDivider(startInset = 14.dp)
                    ChoiceRow(
                        title = label,
                        selected = settings.timeFormat == id,
                        onClick = { viewModel.setTimeFormat(id) }
                    )
                }
            }

            SectionHeader("Date")
            SettingsCard {
                listOf(
                    "system" to "System default",
                    "iso" to "2026-11-13",
                    "dmy" to "13/11/2026",
                    "mdy" to "Nov 13, 2026"
                ).forEachIndexed { index, (id, label) ->
                    if (index > 0) RowDivider(startInset = 14.dp)
                    ChoiceRow(
                        title = label,
                        selected = settings.dateFormat == id,
                        onClick = { viewModel.setDateFormat(id) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun GesturesScreen(viewModel: StillViewModel, settings: Settings, onBack: () -> Unit) {
    ScreenScaffold("Gestures", onBack, subtitle = "Shortcuts on your home screen") {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Home screen")
            SettingsCard {
                ToggleRow(
                    icon = Icons.Outlined.Search,
                    title = "Swipe up to search",
                    subtitle = "Open the app list with the keyboard ready",
                    checked = settings.swipeUpSearch,
                    onCheckedChange = viewModel::setSwipeUpSearch
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Swipe down for notifications",
                    subtitle = "Pull down the notification shade",
                    checked = settings.swipeDownNotifications,
                    onCheckedChange = viewModel::setSwipeDownNotifications
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Outlined.Lock,
                    title = "Double-tap to lock",
                    subtitle = "Needs the focus guard accessibility service",
                    checked = settings.doubleTapLock,
                    onCheckedChange = viewModel::setDoubleTapLock
                )
                RowDivider()
                ToggleRow(
                    icon = StillIcons.Gesture,
                    title = "Keep the home gesture on Home",
                    subtitle = "Returns here if the system overview opens blank. On by default on Infinix, Tecno and itel; needs the focus guard service.",
                    checked = settings.homeGestureGuard,
                    onCheckedChange = viewModel::setHomeGestureGuard
                )
            }

            SectionHeader("App list")
            SettingsCard {
                ToggleRow(
                    icon = StillIcons.Keyboard,
                    title = "Open the keyboard automatically",
                    subtitle = "Focus the search field when the app list opens",
                    checked = settings.autoKeyboard,
                    onCheckedChange = viewModel::setAutoKeyboard
                )
            }

            InfoCard(
                icon = Icons.Outlined.Info,
                title = "Gestures are optional",
                text = "Every shortcut also has a visible button on the home screen, so you can turn gestures off and still find everything."
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
        ) {
            Spacer(Modifier.height(16.dp))
            SettingsCard {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Still Launcher", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusPill("Open source · GPL-3.0", StatusTone.Positive)
                    }
                }
            }

            SectionHeader("What it is", icon = Icons.Outlined.Info)
            SettingsCard {
                Text(
                    "A text-first Android launcher for people who want fewer icons and more intention. " +
                        "Your favourite apps are plain words, everything else is a search away, and optional " +
                        "focus tools step in when an app is taking more time than you planned.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }

            SectionHeader("Privacy", icon = Icons.Outlined.Lock)
            SettingsCard {
                Text(
                    "No account. No analytics. No ads. No network permission is requested. " +
                        "App lists, blocks, limits, notification settings and usage-derived statistics stay " +
                        "in this app's private storage. Usage access and the accessibility service are used " +
                        "only to know which app is currently in the foreground.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }

            SectionHeader("Licence", icon = Icons.Outlined.Create)
            SettingsCard {
                Text(
                    "Source code is released under the GNU General Public License v3.0. " +
                        "The app icon and all text in this project are original. This is an independent " +
                        "clean-room implementation; it shares no code, assets or branding with any " +
                        "commercial product.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun PermissionsScreen(settings: Settings, onBack: () -> Unit, onOpenHelp: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            refresh++
        }
    }
    val status = remember(refresh) { Permissions.readStatus(context) }
    val checkedAt = remember(refresh) { LocalTime.now() }

    ScreenScaffold(
        title = "Permissions",
        onBack = onBack,
        subtitle = "Optional, private and revocable",
        trailing = {
            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Re-check permissions")
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))
            PermissionSummaryCard(status, checkedAt)

            SectionHeader(
                "Essentials",
                "Needed for blocking, limits and the background app indicator",
                StillIcons.Shield
            )
            SettingsCard {
                PermissionCard(
                    icon = Icons.Outlined.Home,
                    title = "Default home app",
                    purpose = "Press Home to return to Still Launcher instead of your old launcher.",
                    granted = status.defaultLauncher,
                    essential = true,
                    actionLabel = "Choose",
                    onAction = { Permissions.openHomeSettings(context) }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = StillIcons.Chart,
                    title = "Usage access",
                    purpose = "Sees which app is open so blocks, limits, recents and screen time can work.",
                    granted = status.usageAccess,
                    essential = true,
                    actionLabel = "Allow",
                    onAction = { Permissions.openUsageAccess(context) }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = StillIcons.Shield,
                    title = "Accessibility service",
                    purpose = "Detects app switches instantly and enables the Recents shortcut.",
                    granted = status.accessibility,
                    essential = true,
                    actionLabel = "Enable",
                    onAction = { Permissions.openAccessibility(context) }
                )
            }

            SectionHeader(
                "Optional",
                "Each one adds a feature. The app works without them.",
                Icons.Outlined.Notifications
            )
            SettingsCard {
                PermissionCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification listener",
                    purpose = "Lets the notification filter remove distractions from the shade.",
                    granted = status.notificationListener,
                    essential = false,
                    actionLabel = "Enable",
                    onAction = { Permissions.openNotificationListener(context) }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Post notifications",
                    purpose = "Shows the small focus guard notification while it is running.",
                    granted = status.postNotifications,
                    essential = false,
                    actionLabel = "Allow",
                    onAction = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = Icons.Outlined.Warning,
                    title = "Display over other apps",
                    purpose = "Lets the block and reminder screens appear over the app you opened.",
                    granted = status.overlay,
                    essential = false,
                    actionLabel = "Allow",
                    onAction = { Permissions.openOverlaySettings(context) }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = StillIcons.Timer,
                    title = "Ignore battery optimisation",
                    purpose = "Helps the focus guard keep running when the phone is idle.",
                    granted = status.ignoringBatteryOptimizations,
                    essential = false,
                    actionLabel = "Allow",
                    onAction = { Permissions.openBatteryOptimization(context) }
                )
                RowDivider(startInset = 14.dp)
                PermissionCard(
                    icon = StillIcons.Contrast,
                    title = "Write secure settings",
                    purpose = "Only for per-app grayscale. Android grants it from a computer, not from this screen.",
                    granted = status.secureSettings,
                    essential = false,
                    actionLabel = null,
                    onAction = { copied = false }
                )
            }

            if (!status.secureSettings) {
                Spacer(Modifier.height(10.dp))
                InfoCard(
                    icon = Icons.Outlined.Info,
                    title = "Grayscale from a computer",
                    text = "Connect this phone with USB debugging enabled, then run this command in a terminal:"
                )
                CodeBlock(
                    code = "adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS",
                    copied = copied,
                    onCopy = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(
                                "adb command",
                                "adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS"
                            )
                        )
                        copied = true
                    }
                )
            }

            TextButton(
                onClick = onOpenHelp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Access greyed out or Play Protect warning? Open help")
            }

            InfoCard(
                icon = Icons.Outlined.Lock,
                title = "You stay in control",
                text = "Revoke any permission in Android Settings at any time. The feature that needs it simply stops; your data stays on this device. Force-stopping the app also pauses enforcement."
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionSummaryCard(status: PermissionStatus, checkedAt: LocalTime) {
    val all = listOf(
        status.defaultLauncher,
        status.usageAccess,
        status.accessibility,
        status.notificationListener,
        status.postNotifications,
        status.overlay,
        status.ignoringBatteryOptimizations,
        status.secureSettings
    )
    val granted = all.count { it }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(StillIcons.Shield)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("$granted of ${all.size} access points on", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Checked ${checkedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} · refreshes automatically",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { granted.toFloat() / all.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (status.essentialsReady) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (status.essentialsReady) {
                    "The three essentials are ready."
                } else {
                    "The three essentials are what blocks and limits need."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    purpose: String,
    granted: Boolean,
    essential: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row {
            IconTile(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    purpose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                text = when {
                    granted -> "On"
                    essential -> "Needed"
                    else -> "Optional"
                },
                tone = when {
                    granted -> StatusTone.Positive
                    essential -> StatusTone.Warning
                    else -> StatusTone.Neutral
                }
            )
            if (!granted && actionLabel != null) {
                Spacer(Modifier.width(10.dp))
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun permissionSummary(status: PermissionStatus): String {
    if (status.essentialsReady) return "All essentials ready"
    val missing = buildList {
        if (!status.defaultLauncher) add("home app")
        if (!status.usageAccess) add("usage access")
        if (!status.accessibility) add("accessibility")
    }
    return "Missing ${missing.joinToString(", ")}"
}

private fun schedulesSummary(settings: Settings): String = when (settings.schedules.size) {
    0 -> "None yet · create a focus window"
    1 -> "1 schedule"
    else -> "${settings.schedules.size} schedules"
}

private fun limitsSummary(settings: Settings): String = when (settings.limits.size) {
    0 -> "None yet · give an app a time budget"
    1 -> "1 app has a daily budget"
    else -> "${settings.limits.size} apps have a daily budget"
}

private fun monochromeSummary(settings: Settings): String = when {
    !settings.monochromeEnabled -> "Off"
    settings.monochromePackages.isEmpty() -> "On · choose apps"
    else -> "On · ${settings.monochromePackages.size} app(s) in grayscale"
}

private fun notificationSummary(settings: Settings): String = when {
    !settings.notificationManagerActive -> "Off"
    settings.notificationAllowed.isEmpty() -> "Active · all notifications pass"
    else -> "Active · ${settings.notificationAllowed.size} allowed app(s)"
}

private fun hiddenSummary(settings: Settings): String = when (settings.hidden.size) {
    0 -> "None hidden"
    1 -> "1 app hidden"
    else -> "${settings.hidden.size} apps hidden"
}

private fun recentsSummary(settings: Settings): String = when (settings.recentApps.size) {
    0 -> "No history · nothing to clear"
    1 -> "1 remembered app · tap to clear"
    else -> "${settings.recentApps.size} remembered apps · tap to clear"
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

@Composable
fun DiagnosticsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val lines by Diagnostics.lines.collectAsStateWithLifecycle()
    var copied by remember { mutableStateOf(false) }
    val isHome = remember { Permissions.isDefaultLauncher(context) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    ScreenScaffold(
        title = "Home gesture diagnostics",
        onBack = onBack,
        subtitle = "In-memory events, never uploaded"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            InfoCard(
                icon = Icons.Outlined.Info,
                title = "How to use this",
                text = "If the Home gesture leaves you on a blank screen, reproduce it once, then come back here and tap Copy log. The log holds window package and class names plus launcher activity events. It stays in memory and is cleared when the app closes."
            )

            SectionHeader("Status")
            SettingsCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Home gesture guard", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (settings.homeGestureGuard) {
                                "On · returns to Home when the overview opens blank"
                            } else {
                                "Off · change it in Settings → Gestures"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        if (settings.homeGestureGuard) "On" else "Off",
                        if (settings.homeGestureGuard) StatusTone.Positive else StatusTone.Neutral
                    )
                }
                RowDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Default home app", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (isHome) "Still Launcher" else "Not selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        if (isHome) "Ready" else "Check",
                        if (isHome) StatusTone.Positive else StatusTone.Warning
                    )
                }
            }

            SectionHeader("Event log", "${lines.size} entries")
            SettingsCard {
                if (lines.isEmpty()) {
                    Text(
                        "No events yet. Use the Home gesture, then open this screen again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                } else {
                    Text(
                        lines.joinToString("\n"),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = if (copied) "Copied" else "Copy log",
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val body = buildString {
                            appendLine("Still Launcher ${BuildConfig.VERSION_NAME}")
                            appendLine("Android ${Build.VERSION.RELEASE} · SDK ${Build.VERSION.SDK_INT}")
                            appendLine("${Build.MANUFACTURER} ${Build.MODEL}")
                            appendLine("default home: $isHome · guard: ${settings.homeGestureGuard}")
                            appendLine("----")
                            lines.forEach(::appendLine)
                        }
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Still Launcher diagnostics", body)
                        )
                        copied = true
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Check
                )
                SecondaryButton(
                    text = "Clear",
                    onClick = { Diagnostics.clear() },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun gestureSummary(settings: Settings): String {
    val enabled = buildList {
        if (settings.swipeUpSearch) add("swipe up")
        if (settings.swipeDownNotifications) add("swipe down")
        if (settings.doubleTapLock) add("double tap")
    }
    return if (enabled.isEmpty()) "All off" else enabled.joinToString(", ")
}
