package org.stilllauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
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
    ScreenScaffold(
        title = "Blocking schedules",
        onBack = onBack,
        subtitle = "Quiet windows that block chosen apps",
        trailing = { AddButton("New") { onEdit(null) } }
    ) {
        if (settings.schedules.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoCard(
                    icon = Icons.Outlined.DateRange,
                    title = "How a schedule works",
                    text = "Pick days and a time window, then choose the apps to block. A window that ends before it starts runs overnight."
                )
                EmptyState(
                    icon = Icons.Outlined.DateRange,
                    title = "No schedules yet",
                    body = "A late-evening or early-morning window is a good place to start.",
                    actionLabel = "Create a schedule",
                    onAction = { onEdit(null) }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    InfoCard(
                        icon = Icons.Outlined.Info,
                        title = "Schedules switch themselves on",
                        text = "Tap a card to change its days, times or apps. Use the switch to pause it without deleting it."
                    )
                }
                items(settings.schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { onEdit(schedule.id) },
                        onToggle = { viewModel.saveSchedule(schedule.copy(enabled = it)) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: BlockSchedule,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(Icons.Outlined.DateRange)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(schedule.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${Formats.daysLabel(schedule.days)} · ${Formats.clock(schedule.startMinute)}–${Formats.clock(schedule.endMinute)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${schedule.packages.size} app${if (schedule.packages.size == 1) "" else "s"} · tap to edit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
        }
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
    val canSave = packages.isNotEmpty() && days.isNotEmpty()

    ScreenScaffold(
        title = if (existing == null) "New schedule" else "Edit schedule",
        onBack = onBack,
        subtitle = "Apps stay blocked during the window"
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            SectionHeader("Repeat")
            SettingsCard {
                Column(Modifier.padding(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..7).forEach { day ->
                            val selected = day in days
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    days = if (selected) days - day else days + day
                                },
                                label = { Text(Formats.dayName(day).take(3)) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                    if (days.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Choose at least one day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SectionHeader("Time")
            SettingsCard {
                NavRow(
                    icon = StillIcons.Clock,
                    title = "Starts",
                    subtitle = Formats.clock(start),
                    onClick = { showStartPicker = true }
                )
                RowDivider()
                NavRow(
                    icon = StillIcons.Clock,
                    title = "Ends",
                    subtitle = Formats.clock(end) + if (end <= start) " · next day" else "",
                    onClick = { showEndPicker = true }
                )
            }

            SectionHeader("Apps")
            SettingsCard {
                NavRow(
                    icon = StillIcons.Grid,
                    title = "Apps to block",
                    subtitle = if (packages.isEmpty()) "No apps selected yet"
                    else "${packages.size} app${if (packages.size == 1) "" else "s"} selected",
                    onClick = { showAppPicker = true }
                )
            }

            if (!canSave) {
                InfoCard(
                    icon = Icons.Outlined.Warning,
                    title = "Almost there",
                    text = "A schedule needs at least one day and one app before it can save.",
                    tone = StatusTone.Warning
                )
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = if (existing == null) "Create schedule" else "Save changes",
                onClick = {
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
                },
                icon = Icons.Outlined.Check,
                enabled = canSave,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (existing != null) {
                TextButton(
                    onClick = { showDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete schedule", color = MaterialTheme.colorScheme.error)
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
            apps = apps.filter { it.isCurrentProfile }.map {
                AppEntry(it.profileKey, viewModel.displayName(it))
            },
            selected = packages,
            multi = true,
            title = "Apps to block",
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
            icon = Icons.Outlined.Delete,
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
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var usedToday by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(settings.limits) {
        while (true) {
            usedToday = withContext(Dispatchers.IO) {
                settings.limits.associate { limit ->
                    limit.packageName to UsageRepository.usageSince(
                        context,
                        limit.packageName,
                        UsageRepository.startOfToday()
                    )
                }
            }
            delay(15_000)
        }
    }

    ScreenScaffold(
        title = "Daily app limits",
        onBack = onBack,
        subtitle = "A budget for apps that pull you in",
        trailing = { AddButton { showPicker = true } }
    ) {
        if (settings.limits.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoCard(
                    icon = StillIcons.Timer,
                    title = "How a daily limit works",
                    text = "Give an app a budget. When it runs out, Still Launcher can remind you, take you out, or pause the app for 15 minutes."
                )
                EmptyState(
                    icon = StillIcons.Timer,
                    title = "No limits yet",
                    body = "Start with the app you most often open without thinking.",
                    actionLabel = "Add a limit",
                    onAction = { showPicker = true }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    InfoCard(
                        icon = Icons.Outlined.Info,
                        title = "Tap a limit to change it",
                        text = "The bar shows today's use against the daily budget."
                    )
                }
                items(settings.limits, key = { it.packageName }) { limit ->
                    LimitCard(
                        label = viewModel.displayName(limit.packageName),
                        limit = limit,
                        usedMillis = usedToday[limit.packageName] ?: 0L,
                        onClick = { onEdit(limit.packageName) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = apps
                .filter { it.isCurrentProfile }
                .filter { app -> settings.limits.none { it.packageName == app.profileKey } }
                .map { AppEntry(it.profileKey, viewModel.displayName(it)) },
            selected = emptySet(),
            multi = false,
            title = "Add a daily limit",
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                selected.firstOrNull()?.let {
                    showPicker = false
                    onEdit(it)
                }
            }
        )
    }
}

@Composable
private fun LimitCard(
    label: String,
    limit: AppLimit,
    usedMillis: Long,
    onClick: () -> Unit
) {
    val budgetMillis = (limit.minutes * 60_000L).coerceAtLeast(1L)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${Formats.minutesLabel(limit.minutes)} per day · ${behaviorLabel(limit.behavior)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${Formats.duration(usedMillis)} used today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            UsageBar(
                fraction = usedMillis.toFloat() / budgetMillis,
                critical = usedMillis >= budgetMillis
            )
        }
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
    var minutes by remember(packageName) {
        mutableFloatStateOf((existing?.minutes ?: 30).toFloat())
    }
    var behavior by remember(packageName) {
        mutableStateOf(existing?.behavior ?: ReminderBehavior.ONLY_REMIND)
    }
    var used by remember { mutableStateOf(0L) }
    val label = viewModel.displayName(packageName)

    LaunchedEffect(packageName) {
        while (true) {
            used = withContext(Dispatchers.IO) {
                UsageRepository.usageSince(context, packageName, UsageRepository.startOfToday())
            }
            delay(5_000)
        }
    }

    ScreenScaffold(
        title = "Daily limit",
        onBack = onBack,
        subtitle = label
    ) {
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
                        "Daily budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepperButton(
                            label = "−",
                            enabled = minutes > 5f,
                            onClick = { minutes = (minutes - 5f).coerceAtLeast(5f) }
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${minutes.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 38.sp)
                            )
                            Text(
                                "minutes per day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        StepperButton(
                            label = "+",
                            enabled = minutes < 240f,
                            onClick = { minutes = (minutes + 5f).coerceAtMost(240f) }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Slider(
                        value = minutes,
                        onValueChange = {
                            minutes = ((it / 5f).roundToInt() * 5).toFloat().coerceIn(5f, 240f)
                        },
                        valueRange = 5f..240f
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { value ->
                            FilterChip(
                                selected = minutes.toInt() == value,
                                onClick = { minutes = value.toFloat() },
                                label = { Text("${value}m") }
                            )
                        }
                    }
                }
            }

            SectionHeader("Today")
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Used today",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${Formats.duration(used)} of ${Formats.minutesLabel(minutes.toInt())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    UsageBar(
                        fraction = used.toFloat() / (minutes * 60_000f).coerceAtLeast(1f),
                        critical = used >= (minutes * 60_000f)
                    )
                }
            }

            SectionHeader("When the budget runs out")
            SettingsCard {
                behaviorOptions().forEachIndexed { index, option ->
                    if (index > 0) RowDivider(startInset = 14.dp)
                    ChoiceRow(
                        title = option.label,
                        description = option.description,
                        selected = behavior == option.value,
                        onClick = { behavior = option.value },
                        icon = option.icon
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = if (existing == null) "Set daily limit" else "Save changes",
                onClick = {
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
                },
                icon = Icons.Outlined.Check,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (existing != null) {
                TextButton(
                    onClick = {
                        viewModel.deleteLimit(packageName)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove limit", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(46.dp)
    ) {
        Text(label, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun MonochromeScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val apps = allApps.filter { it.isCurrentProfile }
    var refresh by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(Permissions.canWriteSecureSettings(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(refresh) {
        hasPermission = Permissions.canWriteSecureSettings(context)
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { viewModel.displayName(it).contains(query, ignoreCase = true) }
    }
    val selectedCount = apps.count { it.profileKey in settings.monochromePackages }

    ScreenScaffold(
        title = "Monochrome mode",
        onBack = onBack,
        subtitle = "Colour off while selected apps are open",
        trailing = {
            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Re-check permission")
            }
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            if (!hasPermission) {
                item {
                    InfoCard(
                        icon = StillIcons.Contrast,
                        title = "A computer is needed for this one",
                        text = "Android only grants secure-settings access from a connected computer. This keeps grayscale optional and prevents other apps from toggling it silently."
                    )
                }
                item {
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
                item {
                    SecondaryButton(
                        text = "Check again",
                        onClick = { refresh++ },
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                item {
                    SettingsCard {
                        ToggleRow(
                            icon = StillIcons.Contrast,
                            title = "Enable monochrome mode",
                            subtitle = "Applies only while selected apps are in front",
                            checked = settings.monochromeEnabled,
                            onCheckedChange = viewModel::setMonochromeEnabled
                        )
                    }
                }
                item {
                    InfoCard(
                        icon = Icons.Outlined.Info,
                        title = "Colour comes back automatically",
                        text = "Leaving a selected app restores colour, and turning the screen off clears grayscale too."
                    )
                }
                item {
                    SectionHeader(
                        "Apps",
                        "$selectedCount of ${apps.size} selected"
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search apps") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "No apps match",
                            body = "Try a different name."
                        )
                    }
                } else {
                    items(filtered, key = { it.profileKey }) { app ->
                        val name = viewModel.displayName(app)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            AppToggleRow(
                                label = name,
                                checked = app.profileKey in settings.monochromePackages,
                                onCheckedChange = {
                                    viewModel.toggleMonochromePackage(app.profileKey)
                                }
                            )
                        }
                    }
                }
            }
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
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val apps = allApps.filter { it.isCurrentProfile }
    var showPicker by remember { mutableStateOf(false) }
    var hasListener by remember {
        mutableStateOf(Permissions.isNotificationListenerEnabled(context))
    }
    val filtered by FilteredNotificationStore.items.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasListener = Permissions.isNotificationListenerEnabled(context)
    }

    ScreenScaffold(
        title = "Notification filter",
        onBack = onBack,
        subtitle = "Keep only the notifications you choose"
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Filter notifications",
                        subtitle = "Only apps on your allow-list may post",
                        checked = settings.notificationManagerActive,
                        onCheckedChange = viewModel::setNotificationManagerActive
                    )
                    RowDivider()
                    NavRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Allowed apps",
                        subtitle = if (settings.notificationAllowed.isEmpty()) {
                            "None selected · everything passes"
                        } else "${settings.notificationAllowed.size} app(s) allowed",
                        onClick = { showPicker = true }
                    )
                }
            }
            if (!hasListener) {
                item {
                    InfoCard(
                        icon = Icons.Outlined.Warning,
                        title = "Notification access is off",
                        text = "The filter cannot see or remove notifications until you grant notification listener access.",
                        tone = StatusTone.Warning
                    )
                }
                item {
                    SecondaryButton(
                        text = "Enable notification access",
                        onClick = { Permissions.openNotificationListener(context) },
                        icon = Icons.Outlined.Notifications,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item {
                InfoCard(
                    icon = Icons.Outlined.Info,
                    title = "What passes",
                    text = "With no allowed apps, every notification passes. Ongoing and foreground-service notifications always pass, so music and calls are never hidden."
                )
            }
            item {
                SectionHeader(
                    "Filtered recently",
                    "Newest first · kept in memory only"
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Nothing has been filtered yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(filtered) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    appLabel(context, item.packageName),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.title.ifBlank { "Notification" },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (item.text.isNotBlank()) {
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { FilteredNotificationStore.clear() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear list")
                    }
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = apps.map {
                AppEntry(it.profileKey, viewModel.displayName(it))
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
fun ScreenTimeScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(Permissions.hasUsageAccess(context)) }
    var range by remember { mutableStateOf("today") }
    var usage by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = Permissions.hasUsageAccess(context)
    }

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

    ScreenScaffold(
        title = "Screen time",
        onBack = onBack,
        subtitle = "Reconstructed from on-device usage events"
    ) {
        if (!hasAccess) {
            EmptyState(
                icon = StillIcons.Chart,
                title = "Usage access is needed",
                body = "Still Launcher reads app usage on this device to show screen time. Nothing is uploaded.",
                actionLabel = "Allow usage access",
                onAction = { Permissions.openUsageAccess(context) }
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    SettingsCard {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "Total screen time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Formats.duration(total),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("today" to "Today", "week" to "Week", "month" to "Month")
                                    .forEach { (id, label) ->
                                        FilterChip(
                                            selected = range == id,
                                            onClick = { range = id },
                                            label = { Text(label) }
                                        )
                                    }
                            }
                        }
                    }
                }
                item {
                    SectionHeader(
                        "Most used",
                        if (usage.isEmpty()) "No usage recorded yet" else "${usage.size} app(s) used in this period"
                    )
                }
                if (usage.isEmpty()) {
                    item {
                        EmptyState(
                            icon = StillIcons.Chart,
                            title = "No usage yet",
                            body = "Use a few apps, then come back. Data appears after Android records some activity."
                        )
                    }
                } else {
                    val max = usage.maxOfOrNull { it.second } ?: 1L
                    items(usage.take(30), key = { it.first }) { (pkg, millis) ->
                        val label = viewModel.displayName(pkg)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        Formats.duration(millis),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                UsageBar(millis.toFloat() / max.coerceAtLeast(1L))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HiddenAppsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onBack: () -> Unit
) {
    ScreenScaffold(
        title = "Hidden apps",
        onBack = onBack,
        subtitle = "Installed, but kept out of your app list"
    ) {
        if (settings.hidden.isEmpty()) {
            EmptyState(
                icon = StillIcons.EyeOff,
                title = "No hidden apps",
                body = "Long-press an app in All apps and choose Hide from app list. You can bring it back here."
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(settings.hidden) { pkg ->
                    val label = viewModel.appForKey(pkg)?.let { viewModel.displayName(it) }
                        ?: viewModel.displayName(pkg)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TextButton(onClick = { viewModel.unhide(pkg) }) {
                                Text("Unhide")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private data class BehaviorOption(
    val value: ReminderBehavior,
    val label: String,
    val description: String,
    val icon: ImageVector
)

private fun behaviorOptions(): List<BehaviorOption> = listOf(
    BehaviorOption(
        ReminderBehavior.ONLY_REMIND,
        "Only remind",
        "Show a check-in with the option to stay or leave",
        Icons.Outlined.Notifications
    ),
    BehaviorOption(
        ReminderBehavior.EXIT_APP,
        "Encourage leaving",
        "Show a stronger prompt when the budget is reached",
        Icons.Outlined.ExitToApp
    ),
    BehaviorOption(
        ReminderBehavior.COOLDOWN,
        "Pause for 15 minutes",
        "Block the app briefly so the habit loop is interrupted",
        Icons.Outlined.Lock
    )
)

private fun behaviorLabel(behavior: ReminderBehavior): String = when (behavior) {
    ReminderBehavior.ONLY_REMIND -> "remind me"
    ReminderBehavior.EXIT_APP -> "encourage leaving"
    ReminderBehavior.COOLDOWN -> "pause 15 min"
}

private fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
