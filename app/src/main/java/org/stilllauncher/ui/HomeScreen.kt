package org.stilllauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings
import org.stilllauncher.util.Formats
import org.stilllauncher.util.HomeGestureGuard
import org.stilllauncher.util.SystemActions

@Composable
fun HomeScreen(
    viewModel: StillViewModel,
    settings: Settings,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetLimit: (String) -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember { mutableStateOf(-1) }
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var dragY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) battery = level * 100 / scale
            }
        }
        val sticky = context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (sticky != null) {
            val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) battery = level * 100 / scale
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(settings.swipeUpSearch, settings.swipeDownNotifications) {
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, amount -> dragY += amount },
                    onDragEnd = {
                        if (dragY < -110f && settings.swipeUpSearch) {
                            onOpenAllApps()
                        } else if (dragY > 110f && settings.swipeDownNotifications) {
                            SystemActions.expandNotifications(context)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapLock) SystemActions.lockScreen()
                    }
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            ClockCircle(settings, now, battery)
            Spacer(Modifier.height(14.dp))

            val recent = remember(settings.recentApps, settings.hidden, apps) {
                settings.recentApps.firstNotNullOfOrNull { rec ->
                    val app = apps.firstOrNull { it.profileKey == rec.packageName }
                    if (app != null && rec.packageName !in settings.hidden) app to rec else null
                }
            }
            recent?.let { (app, rec) ->
                RecentAppCard(
                    label = viewModel.displayName(app),
                    lastUsed = rec.lastUsed,
                    onReturn = { viewModel.launch(app) }
                )
                Spacer(Modifier.height(10.dp))
            }

            val favouriteApps = remember(settings.favorites, apps) {
                settings.favorites.mapNotNull { key ->
                    apps.firstOrNull { it.profileKey == key }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (favouriteApps.isEmpty()) {
                    FavouritesEmpty(
                        onOpenAllApps = onOpenAllApps,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(favouriteApps.take(11), key = { it.profileKey }) { app ->
                            AppRow(
                                label = viewModel.displayName(app),
                                onClick = { viewModel.launch(app) },
                                onLongClick = { selected = app }
                            )
                        }
                    }
                }
            }

            HomeDock(
                onCall = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                onRecents = {
                    // A deliberate Recents tap must not be mistaken for the
                    // OEM's blank overview after the Home gesture.
                    HomeGestureGuard.suppress()
                    if (!SystemActions.openRecents()) {
                        Toast.makeText(
                            context,
                            "Enable the focus guard accessibility service to open recents",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onAllApps = onOpenAllApps,
                onSettings = onOpenSettings
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    selected?.let { app ->
        AppActionsDialog(
            viewModel = viewModel,
            settings = settings,
            app = app,
            onDismiss = { selected = null },
            onSetLimit = {
                selected = null
                onSetLimit(it)
            }
        )
    }
}

@Composable
private fun RecentAppCard(label: String, lastUsed: Long, onReturn: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Background · ${Formats.ago(lastUsed)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onReturn,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun FavouritesEmpty(onOpenAllApps: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your favourites live here", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Open All apps, then long-press an app and choose Favourite. It will appear on this screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onOpenAllApps,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Browse all apps")
            }
        }
    }
}

@Composable
private fun HomeDock(
    onCall: () -> Unit,
    onRecents: () -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockTextItem("Call", onCall)
            DockTextItem("Recents", onRecents)
            DockTextItem("All apps", onAllApps, emphasized = true)
            DockTextItem("Settings", onSettings)
        }
    }
}

@Composable
private fun RowScope.DockTextItem(
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (emphasized) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ClockCircle(settings: Settings, now: LocalDateTime, battery: Int) {
    val foreground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = foreground.copy(alpha = 0.14f),
                style = Stroke(width = stroke)
            )
            if (battery >= 0) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * (battery / 100f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Formats.time(settings, now),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (settings.fontSizeSp * 2.35f).sp
                )
            )
            Text(
                Formats.date(settings, now.toLocalDate()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (battery >= 0) {
                Text(
                    "$battery%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
