package org.stilllauncher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.stilllauncher.MainActivity
import org.stilllauncher.data.Settings
import org.stilllauncher.data.SettingsStore
import org.stilllauncher.ui.theme.StillTheme
import org.stilllauncher.util.Formats
import org.stilllauncher.util.SystemActions
import org.stilllauncher.util.appLabel

class BlockedActivity : ComponentActivity() {

    private var packageName: String = ""
    private var until: Long = 0L
    private var reason: String = "Blocked"
    private val store by lazy { SettingsStore.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = Settings())
            StillTheme(settings) {
                BlockedContent(
                    packageName = packageName,
                    until = until,
                    reason = reason,
                    canEndEarly = reason == "Blocked",
                    onGoHome = {
                        SystemActions.goHome(this)
                        finish()
                    },
                    onEndBlock = {
                        lifecycleScope.launch {
                            val s = store.settings.first()
                            store.setBlocked(s.blocked.filterNot { it.packageName == packageName })
                            finish()
                        }
                    },
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
        recreate()
    }

    private fun readIntent(intent: Intent?) {
        packageName = intent?.getStringExtra(EXTRA_PACKAGE) ?: ""
        until = intent?.getLongExtra(EXTRA_UNTIL, 0L) ?: 0L
        reason = intent?.getStringExtra(EXTRA_REASON) ?: "Blocked"
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
        const val EXTRA_UNTIL = "blocked_until"
        const val EXTRA_REASON = "blocked_reason"
    }
}

@Composable
private fun BlockedContent(
    packageName: String,
    until: Long,
    reason: String,
    canEndEarly: Boolean,
    onGoHome: () -> Unit,
    onEndBlock: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var confirmEnd by remember { mutableStateOf(false) }
    val label = remember(packageName) { appLabel(context, packageName) }

    LaunchedEffect(until) {
        while (true) {
            now = System.currentTimeMillis()
            if (until > 0 && now >= until) {
                onGoHome()
                return@LaunchedEffect
            }
            delay(1_000)
        }
    }

    BackHandler {
        onGoHome()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (canEndEarly) "This app is blocked." else "Focus time: $reason.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        if (until > 0) {
            Text(
                Formats.relativeUntil(until, now),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                "until " + Formats.time(Settings(), java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(until),
                    java.time.ZoneId.systemDefault()
                )),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(36.dp))
        Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
            Text("Go to home screen")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenApp, modifier = Modifier.fillMaxWidth()) {
            Text("Open Still Launcher")
        }
        if (canEndEarly) {
            TextButton(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End block early")
            }
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "End the block?",
            message = "The point of a block is to create space. Ending it early is allowed, but make it a deliberate choice.",
            confirmLabel = "End block",
            destructive = true,
            onDismiss = { confirmEnd = false },
            onConfirm = onEndBlock
        )
    }
}
