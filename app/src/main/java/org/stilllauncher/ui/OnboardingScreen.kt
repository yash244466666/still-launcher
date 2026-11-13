package org.stilllauncher.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import org.stilllauncher.util.PermissionStatus
import org.stilllauncher.util.Permissions

private data class IntroPage(
    val title: String,
    val body: String,
    val icon: ImageVector
)

private val introPages = listOf(
    IntroPage(
        "A quieter home screen",
        "No icon grid. Your favourite apps are plain words, and everything else is one search away.",
        Icons.Outlined.Home
    ),
    IntroPage(
        "Make room for focus",
        "Give an app a daily budget, block it for a while, or set quiet hours. Still Launcher notices when you open it and steps in.",
        StillIcons.Shield
    ),
    IntroPage(
        "Private by design",
        "No account, no analytics, no ads, no network permission. Usage access and accessibility are used only to know which app is open.",
        Icons.Outlined.Lock
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var refresh by remember { mutableStateOf(0) }
    val last = page == introPages.lastIndex

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            refresh++
        }
    }
    val status = remember(refresh) { Permissions.readStatus(context) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Still Launcher", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (!last) {
                TextButton(onClick = onFinished) { Text("Skip") }
            }
        }
        Spacer(Modifier.height(10.dp))
        PageDots(page)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier
                    .size(74.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    introPages[page].icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                introPages[page].title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                introPages[page].body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (last) {
                Spacer(Modifier.height(22.dp))
                SetupChecklist(status)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (last) {
            PrimaryButton(
                text = "Start using Still Launcher",
                onClick = onFinished,
                icon = Icons.Outlined.Check
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You can change these any time in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (page > 0) page -= 1 },
                    enabled = page > 0
                ) { Text("Back") }
                Button(
                    onClick = { page += 1 },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (page == introPages.lastIndex - 1) "Set up" else "Next")
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(introPages.size) { index ->
            val selected = index == page
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                label = "pageDotWidth"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SetupChecklist(status: PermissionStatus) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Three quick steps",
            style = MaterialTheme.typography.titleMedium
        )
        SetupAccessRow(
            icon = Icons.Outlined.Home,
            title = "Make this your home screen",
            subtitle = "Press Home to come back here",
            granted = status.defaultLauncher,
            actionLabel = "Choose",
            onAction = { Permissions.openHomeSettings(context) }
        )
        SetupAccessRow(
            icon = StillIcons.Chart,
            title = "Allow usage access",
            subtitle = "Detects the app you are using",
            granted = status.usageAccess,
            actionLabel = "Allow",
            onAction = { Permissions.openUsageAccess(context) }
        )
        SetupAccessRow(
            icon = StillIcons.Shield,
            title = "Enable the focus guard",
            subtitle = "Instant detection and the Recents shortcut",
            granted = status.accessibility,
            actionLabel = "Enable",
            onAction = { Permissions.openAccessibility(context) }
        )
    }
}

@Composable
private fun SetupAccessRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Ready",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
