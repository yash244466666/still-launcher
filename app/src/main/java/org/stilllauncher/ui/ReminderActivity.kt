package org.stilllauncher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.stilllauncher.data.ReminderBehavior
import org.stilllauncher.data.Settings
import org.stilllauncher.data.SettingsStore
import org.stilllauncher.ui.theme.StillTheme
import org.stilllauncher.util.Formats
import org.stilllauncher.util.SystemActions
import org.stilllauncher.util.appLabel

class ReminderActivity : ComponentActivity() {

    private var packageName: String = ""
    private var usedMillis: Long = 0L
    private var limitMinutes: Int = 0
    private var behavior: ReminderBehavior = ReminderBehavior.ONLY_REMIND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        usedMillis = intent.getLongExtra(EXTRA_USED, 0L)
        limitMinutes = intent.getIntExtra(EXTRA_LIMIT, 0)
        behavior = runCatching {
            ReminderBehavior.valueOf(
                intent.getStringExtra(EXTRA_BEHAVIOR) ?: ReminderBehavior.ONLY_REMIND.name
            )
        }.getOrDefault(ReminderBehavior.ONLY_REMIND)

        setContent {
            val settings by SettingsStore.get(this).settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            StillTheme(settings) {
                ReminderContent(
                    packageName = packageName,
                    usedMillis = usedMillis,
                    limitMinutes = limitMinutes,
                    behavior = behavior,
                    onLeave = {
                        SystemActions.goHome(this)
                        finish()
                    },
                    onStay = { finish() },
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "reminder_package"
        const val EXTRA_USED = "reminder_used"
        const val EXTRA_LIMIT = "reminder_limit"
        const val EXTRA_BEHAVIOR = "reminder_behavior"
    }
}

@Composable
private fun ReminderContent(
    packageName: String,
    usedMillis: Long,
    limitMinutes: Int,
    behavior: ReminderBehavior,
    onLeave: () -> Unit,
    onStay: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val label = remember(packageName) { appLabel(context, packageName) }

    BackHandler { onDismiss() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(23.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                StillIcons.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Time to check in",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "You have spent ${Formats.duration(usedMillis)} on $label today. Your daily budget is ${Formats.minutesLabel(limitMinutes)}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onLeave,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Leave the app")
        }
        Spacer(Modifier.height(8.dp))
        if (behavior == ReminderBehavior.EXIT_APP) {
            TextButton(onClick = onStay, modifier = Modifier.fillMaxWidth()) {
                Text("Stay anyway")
            }
        } else {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Remind me again in 10 minutes")
            }
        }
    }
}
