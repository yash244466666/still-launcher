package org.stilllauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.stilllauncher.util.Permissions

private data class IntroPage(val title: String, val body: String)

private val introPages = listOf(
    IntroPage(
        "A quieter home screen",
        "No icon grid. Your favourite apps are plain words, ordered the way you think about them. Everything else is a search away."
    ),
    IntroPage(
        "Make room for focus",
        "Block an app for an hour or a month, set morning and evening focus windows, or give an app a daily time budget. Still Launcher notices when you open it and steps in."
    ),
    IntroPage(
        "Private by design",
        "No account, no analytics, no ads. Your app list, blocks and limits stay in this device's private storage. Usage access and the accessibility service are used only to detect the app you are currently using."
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    val last = page == introPages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "Still Launcher",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 30.sp
        )
        Spacer(Modifier.height(42.dp))
        Text(
            introPages[page].title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(14.dp))
        Text(
            introPages[page].body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.weight(1f))

        if (last) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { Permissions.openHomeSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Set Still Launcher as your home app") }
                TextButton(
                    onClick = { Permissions.openUsageAccess(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow usage access") }
                TextButton(
                    onClick = { Permissions.openAccessibility(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enable the focus guard service") }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                Text("Start using Still Launcher")
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onFinished) { Text("Skip") }
                Button(onClick = { page += 1 }) { Text("Next") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${page + 1} / ${introPages.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
