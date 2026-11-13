package org.stilllauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.AppLimit
import org.stilllauncher.data.BlockSchedule
import org.stilllauncher.data.ReminderBehavior
import org.stilllauncher.data.Settings
import org.stilllauncher.data.UsageRepository
import org.stilllauncher.service.FilteredNotificationStore
import org.stilllauncher.util.Formats
import org.stilllauncher.util.Permissions

@Composable
fun SchedulesScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit,
    onEdit: (String?) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<BlockSchedule?>(null) }
    ScreenScaffold(
        title = "Blocking schedules",
        onBack = onBack,
        trailing = { TextButton(onClick = { onEdit(null) }) { Text("Add") } }
    ) {
        if (settings.schedules.isEmpty()) {
            EmptyHint(
                "A schedule blocks a set of apps during times you choose, for example late evening or first thing in the morning."
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(settings.schedules, key = { it.id }) { schedule ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(schedule.id) }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(schedule.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${Formats.daysLabel(schedule.days)} · ${Formats.clock(schedule.startMinute)}–${Formats.clock(schedule.endMinute)} · ${schedule.packages.size} app(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = schedule.enabled,
                                onCheckedChange = {
                                    viewModel.saveSchedule(schedule.copy(enabled = it))
                                }
                            )
                        }
                        Row(Modifier.padding(start = 12.dp, bottom = 6.dp)) {
                            TextButton(onClick = { onEdit(schedule.id) }) { Text("Edit") }
                            TextButton(onClick = { deleteTarget = schedule }) { Text("Delete") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
    deleteTarget?.let { schedule ->
        ConfirmDialog(
            title = "Delete schedule?",
            message = "\"${schedule.name}\" will stop blocking apps.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { deleteTarget = null },
            onConfirm = { viewModel.deleteSchedule(schedule.id) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorScreen(
    viewModel: StillViewModel,
    settings: Settings,
    scheduleId: String?,
    onBack: () -> Unit
) {
    val existing = settings.schedules.firstOrNull { it.id == scheduleId }
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var name by remember(scheduleId) { mutableStateOf(existing?.name ?: "Focus time") }
    var days by remember(scheduleId) { mutableStateOf(existing?.days ?: setOf(1, 2, 3, 4, 5, 6, 7)) }
    var start by remember(scheduleId) { mutableStateOf(existing?.startMinute ?: (22 * 60)) }
    var end by remember(scheduleId) { mutableStateOf(existing?.endMinute ?: (7 * 60)) }
    var packages by remember(scheduleId) {
        mutableStateOf(existing?.packages?.toSet() ?: emptySet())
    }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    ScreenScaffold("${if (existing == null) "New" else "Edit"} schedule", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
            SectionLabel("Days")
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..7).forEach { day ->
                    FilterChip(
                        selected = day in days,
                        onClick = {
                            days = if (day in days) days - day else days + day
                        },
                        label = { Text(Formats.dayName(day).take(3)) }
                    )
                }
            }
            SectionLabel("Time")
            SettingRow(
                "Starts",
                subtitle = Formats.clock(start),
                onClick = { showStartPicker = true }
            )
            SettingRow(
                "Ends",
                subtitle = Formats.clock(end) + if (end <= start) " (next day)" else "",
                onClick = { showEndPicker = true }
            )
            SettingRow(
                "Apps",
                subtitle = "${packages.size} selected",
                onClick = { showAppPicker = true }
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = {
                    val id = existing?.id ?: java.util.UUID.randomUUID().toString()
                    viewModel.saveSchedule(
                        BlockSchedule(
                            id = id,
                            name = name.ifBlank { "Focus time" },
                            enabled = existing?.enabled ?: true,
                            days = days,
                            startMinute = start,
                            endMinute = end,
                            packages = packages.toList()
                        )
                    )
                    onBack()
                }) { Text("Save") }
                if (existing != null) {
                    TextButton(onClick = { showDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showStartPicker) {
        MinutePickerDialog(
            initialMinute = start,
            onDismiss = { showStartPicker = false },
            onConfirm = { start = it }
        )
    }
    if (showEndPicker) {
        MinutePickerDialog(
            initialMinute = end,
            onDismiss = { showEndPicker = false },
            onConfirm = { end = it }
        )
    }
    if (showAppPicker) {
        AppPickerDialog(
            apps = apps.map {
                AppEntry(it.packageName, viewModel.displayName(it.packageName))
            },
            selected = packages,
            multi = true,
            onDismiss = { showAppPicker = false },
            onConfirm = { packages = it }
        )
    }
    if (showDelete && existing != null) {
        ConfirmDialog(
            title = "Delete schedule?",
            message = "\"${existing.name}\" will stop blocking apps.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { showDelete = false },
            onConfirm = {
                viewModel.deleteSchedule(existing.id)
                onBack()
            }
        )
    }
}

@Composable
fun LimitsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = "Daily app limits",
        onBack = onBack,
        trailing = { TextButton(onClick = { showPicker = true }) { Text("Add") } }
    ) {
        if (settings.limits.isEmpty()) {
            EmptyHint(
                "Give an app a daily time budget. When the budget runs out, Still Launcher can remind you, take you out of the app, or pause it for 15 minutes."
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(settings.limits, key = { it.packageName }) { limit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(limit.packageName) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                viewModel.displayName(limit.packageName),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "${Formats.minutesLabel(limit.minutes)} / day · ${behaviorLabel(limit.behavior)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { deleteTarget = limit.packageName }) {
                            Text("Delete")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = apps
                .filter { app -> settings.limits.none { it.packageName == app.packageName } }
                .map { AppEntry(it.packageName, viewModel.displayName(it.packageName)) },
            selected = emptySet(),
            multi = false,
            title = "Add a limit",
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                selected.firstOrNull()?.let {
                    showPicker = false
                    onEdit(it)
                }
            }
        )
    }
    deleteTarget?.let { pkg ->
        ConfirmDialog(
            title = "Remove limit?",
            message = "No reminder or block will apply to ${viewModel.displayName(pkg)}.",
            confirmLabel = "Remove",
            destructive = true,
            onDismiss = { deleteTarget = null },
            onConfirm = { viewModel.deleteLimit(pkg) }
        )
    }
}

@Composable
fun LimitEditorScreen(
    viewModel: StillViewModel,
    settings: Settings,
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val existing = settings.limits.firstOrNull { it.packageName == packageName }
    var minutes by remember(packageName) { mutableStateOf((existing?.minutes ?: 30).toFloat()) }
    var behavior by remember(packageName) {
        mutableStateOf(existing?.behavior ?: ReminderBehavior.ONLY_REMIND)
    }
    var used by remember { mutableStateOf(0L) }

    LaunchedEffect(packageName) {
        while (true) {
            used = withContext(Dispatchers.IO) {
                UsageRepository.usageSince(context, packageName, UsageRepository.startOfToday())
            }
            delay(5_000)
        }
    }

    ScreenScaffold("Daily limit", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                viewModel.displayName(packageName),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            Text(
                "Used today: ${Formats.duration(used)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            SectionLabel("Daily budget")
            Text(
                Formats.minutesLabel(minutes.toInt()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                valueRange = 5f..240f,
                steps = 46,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            SectionLabel("When the budget runs out")
            behaviorOptions().forEach { (value, label, description) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { behavior = value }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (behavior == value) "●" else "○",
                        color = if (behavior == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = {
                    viewModel.saveLimit(
                        AppLimit(
                            packageName = packageName,
                            minutes = minutes.toInt(),
                            behavior = behavior,
                            lastRemindedAt = existing?.lastRemindedAt ?: 0L,
                            cooldownUntil = existing?.cooldownUntil ?: 0L
                        )
                    )
                    onBack()
                }) { Text("Save") }
                if (existing != null) {
                    TextButton(onClick = {
                        viewModel.deleteLimit(packageName)
                        onBack()
                    }) { Text("Remove limit", color = MaterialTheme.colorScheme.error) }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MonochromeScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var refresh by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val hasPermission = remember(refresh) { Permissions.canWriteSecureSettings(context) }

    ScreenScaffold(
        title = "Monochrome mode",
        onBack = onBack,
        trailing = { TextButton(onClick = { refresh++ }) { Text("Refresh") } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Colour makes engaging apps more engaging. Monochrome mode turns the screen grayscale while selected apps are in front, and restores colour when you leave them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            if (!hasPermission) {
                Text(
                    "This feature needs a permission Android only grants from a computer. Enable USB debugging, connect this phone, then run:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp)
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
            } else {
                SwitchRow(
                    title = "Enable monochrome mode",
                    subtitle = "Applies to the apps selected below",
                    checked = settings.monochromeEnabled,
                    onCheckedChange = viewModel::setMonochromeEnabled
                )
                SectionLabel("Apps")
                if (apps.isEmpty()) {
                    EmptyHint("No launchable apps found.")
                } else {
                    apps.forEach { app ->
                        SwitchRow(
                            title = viewModel.displayName(app.packageName),
                            checked = app.packageName in settings.monochromePackages,
                            onCheckedChange = {
                                viewModel.toggleMonochromePackage(app.packageName)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    val filtered by FilteredNotificationStore.items.collectAsStateWithLifecycle()

    ScreenScaffold("Notification filter", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SwitchRow(
                title = "Filter notifications",
                subtitle = "Only apps you allow may post while this is on",
                checked = settings.notificationManagerActive,
                onCheckedChange = viewModel::setNotificationManagerActive
            )
            SettingRow(
                "Allowed apps",
                subtitle = if (settings.notificationAllowed.isEmpty()) {
                    "None selected — all notifications pass"
                } else "${settings.notificationAllowed.size} app(s)",
                onClick = { showPicker = true }
            )
            Text(
                "Notifications from apps that are not on the list are removed from the shade as soon as they arrive. Ongoing and foreground-service notifications always pass. This screen needs the notification listener permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )

            SectionLabel("Filtered recently")
            if (filtered.isEmpty()) {
                EmptyHint("Nothing filtered yet.")
            } else {
                filtered.forEach { item ->
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            item.title.ifBlank { appLabel(context, item.packageName) },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (item.text.isNotBlank()) {
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            item.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { FilteredNotificationStore.clear() }) {
                    Text("Clear list")
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = apps.map {
                AppEntry(it.packageName, viewModel.displayName(it.packageName))
            },
            selected = settings.notificationAllowed,
            multi = true,
            title = "Apps allowed to notify",
            onDismiss = { showPicker = false },
            onConfirm = { viewModel.setNotificationAllowed(it) }
        )
    }
}

@Composable
fun ScreenTimeScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    var range by remember { mutableStateOf("today") }
    var usage by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    val hasAccess = Permissions.hasUsageAccess(context)

    LaunchedEffect(range, hasAccess) {
        if (!hasAccess) return@LaunchedEffect
        total = 0L
        usage = emptyList()
        while (true) {
            val start = when (range) {
                "week" -> UsageRepository.startOfWeek()
                "month" -> UsageRepository.startOfMonth()
                else -> UsageRepository.startOfToday()
            }
            val result = withContext(Dispatchers.IO) {
                UsageRepository.sortedUsage(context, start)
            }
            usage = result
            total = result.sumOf { it.second }
            delay(15_000)
        }
    }

    ScreenScaffold("Screen time", onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (!hasAccess) {
                Text(
                    "Usage access is required to read screen-time statistics on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp)
                )
                TextButton(onClick = { Permissions.openUsageAccess(context) }) {
                    Text("Grant usage access")
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("today" to "Today", "week" to "Week", "month" to "Month")
                        .forEach { (id, label) ->
                            FilterChip(
                                selected = range == id,
                                onClick = { range = id },
                                label = { Text(label) }
                            )
                        }
                }
                Text(
                    Formats.duration(total),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                val max = usage.maxOfOrNull { it.second } ?: 1L
                usage.take(20).forEach { (pkg, millis) ->
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                appLabel(context, pkg),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Formats.duration(millis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        UsageBar(fraction = millis.toFloat() / max.coerceAtLeast(1L))
                    }
                }
                if (usage.isEmpty()) {
                    EmptyHint("No usage recorded for this period yet.")
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun HiddenAppsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    ScreenScaffold("Hidden apps", onBack) {
        if (settings.hidden.isEmpty()) {
            EmptyHint("No hidden apps. Hide an app from its long-press menu.")
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                settings.hidden.forEach { pkg ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            viewModel.displayName(pkg),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { viewModel.unhide(pkg) }) { Text("Unhide") }
                    }
                }
            }
        }
    }
}

private fun behaviorOptions(): List<Triple<ReminderBehavior, String, String>> = listOf(
    Triple(
        ReminderBehavior.ONLY_REMIND,
        "Only remind",
        "Show a reminder with options to stay or leave"
    ),
    Triple(
        ReminderBehavior.EXIT_APP,
        "Exit app",
        "Encourage leaving the app when the budget is reached"
    ),
    Triple(
        ReminderBehavior.COOLDOWN,
        "Block for 15 minutes",
        "Pause the app after the budget is used"
    )
)

private fun behaviorLabel(behavior: ReminderBehavior): String = when (behavior) {
    ReminderBehavior.ONLY_REMIND -> "remind"
    ReminderBehavior.EXIT_APP -> "exit"
    ReminderBehavior.COOLDOWN -> "15 min block"
}

private fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
