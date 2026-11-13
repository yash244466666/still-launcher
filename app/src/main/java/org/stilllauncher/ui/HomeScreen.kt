package org.stilllauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapLock) SystemActions.lockScreen()
                    }
                )
            }
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
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(26.dp))
            ClockCircle(settings, now, battery)
            Spacer(Modifier.height(16.dp))

            val recent = remember(settings.recentApps, settings.hidden, apps) {
                settings.recentApps.firstOrNull { rec ->
                    rec.packageName !in settings.hidden &&
                        apps.any { it.packageName == rec.packageName }
                }
            }
            recent?.let { rec ->
                RecentAppBar(
                    label = viewModel.displayName(rec.packageName),
                    lastUsed = rec.lastUsed,
                    onReturn = { viewModel.launchPackage(rec.packageName) }
                )
                Spacer(Modifier.height(10.dp))
            }

            val favouriteApps = remember(settings.favorites, apps) {
                settings.favorites.mapNotNull { pkg ->
                    apps.firstOrNull { it.packageName == pkg }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (favouriteApps.isEmpty()) {
                    Text(
                        "Long-press an app in the app list to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        favouriteApps.take(11).forEach { app ->
                            AppRow(
                                label = viewModel.displayName(app.packageName),
                                onClick = { viewModel.launch(app) },
                                onLongClick = { selected = app }
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text("Call") }
                Row {
                    TextButton(onClick = {
                        if (!SystemActions.openRecents()) {
                            Toast.makeText(
                                context,
                                "Enable the focus guard accessibility service to open recents",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) { Text("Recents") }
                    TextButton(onClick = onOpenAllApps) { Text("All apps") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                }
            }
            Spacer(Modifier.height(6.dp))
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
private fun RecentAppBar(label: String, lastUsed: Long, onReturn: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onReturn)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "●",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "In the background · ${Formats.ago(lastUsed)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "Return",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ClockCircle(settings: Settings, now: LocalDateTime, battery: Int) {
    val foreground = MaterialTheme.colorScheme.onBackground
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = foreground.copy(alpha = 0.14f),
                style = Stroke(width = stroke)
            )
            if (battery >= 0) {
                drawArc(
                    color = foreground,
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
